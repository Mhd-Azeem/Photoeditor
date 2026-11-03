#include <jni.h>
#include <android/bitmap.h>
#include <net.h>
#include <gpu.h>
#include <algorithm>
#include <memory>
#include <mutex>
#include <string>
#include <vector>
#include <cmath>

namespace {
std::once_flag gpuInit;
void fail(JNIEnv* env, const char* message) {
    env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), message);
}
struct Engine {
    ncnn::Net net;
    bool gpu;
};
struct Pixels {
    JNIEnv* env; jobject bitmap; void* ptr = nullptr;
    AndroidBitmapInfo info{};
    Pixels(JNIEnv* e, jobject b):env(e),bitmap(b) {
        if (AndroidBitmap_getInfo(e,b,&info)!=0 || info.format!=ANDROID_BITMAP_FORMAT_RGBA_8888 ||
            AndroidBitmap_lockPixels(e,b,&ptr)!=0) ptr=nullptr;
    }
    ~Pixels() { if(ptr) AndroidBitmap_unlockPixels(env,bitmap); }
};
}
extern "C" JNIEXPORT jlong JNICALL
Java_com_photoeditor_ai_OfflineUpscaler_nativeCreate(JNIEnv* env,jobject,jstring param,jstring model,jboolean gpu) {
    try {
    std::call_once(gpuInit, [] { ncnn::create_gpu_instance(); });
    const char* p=env->GetStringUTFChars(param,nullptr);
    const char* m=env->GetStringUTFChars(model,nullptr);
    auto engine=std::make_unique<Engine>();
    engine->gpu=gpu && ncnn::get_gpu_count()>0;
    engine->net.opt.use_vulkan_compute=engine->gpu;
    engine->net.opt.use_fp16_packed=engine->gpu;
    engine->net.opt.use_fp16_storage=engine->gpu;
    engine->net.opt.use_fp16_arithmetic=false;
    engine->net.opt.num_threads=2;
    engine->net.opt.lightmode=true;
    if(engine->gpu) engine->net.set_vulkan_device(ncnn::get_default_gpu_index());
    int status=engine->net.load_param(p);
    if(status==0) status=engine->net.load_model(m);
    env->ReleaseStringUTFChars(param,p); env->ReleaseStringUTFChars(model,m);
    if(status!=0) { fail(env,"RealESRGAN model could not be loaded. No enhancement was produced."); return 0; }
    return reinterpret_cast<jlong>(engine.release());
    } catch (const std::bad_alloc&) { fail(env,"Not enough native memory to load AI model"); return 0; }
      catch (...) { fail(env,"Native AI initialization failed"); return 0; }
}
extern "C" JNIEXPORT jboolean JNICALL
Java_com_photoeditor_ai_OfflineUpscaler_nativeUsesGpu(JNIEnv*,jobject,jlong handle) {
    return reinterpret_cast<Engine*>(handle)->gpu;
}
extern "C" JNIEXPORT void JNICALL
Java_com_photoeditor_ai_OfflineUpscaler_nativeDestroy(JNIEnv*,jobject,jlong handle) {
    delete reinterpret_cast<Engine*>(handle);
}
extern "C" JNIEXPORT void JNICALL
Java_com_photoeditor_ai_OfflineUpscaler_nativeTile(JNIEnv* env,jobject,jlong handle,jobject input,jobject output,jint x,jint y) {
    try {
    auto* e=reinterpret_cast<Engine*>(handle);
    Pixels src(env,input),dst(env,output);
    if(!src.ptr||!dst.ptr) { fail(env,"Cannot access image pixels"); return; }
    const int w=src.info.width,h=src.info.height;
    if(x<0||y<0||x>=w||y>=h||dst.info.width!=unsigned(w*4)||dst.info.height!=unsigned(h*4)) {
        fail(env,"Invalid inference dimensions");return;
    }
    const int cw=std::min(64,w-x),ch=std::min(64,h-y);
    // 32 input pixels of context on every edge. Replicate the outer image boundary.
    const int pad=32,tw=cw+2*pad,th=ch+2*pad;
    std::vector<unsigned char> rgb(tw*th*3);
    for(int j=0;j<th;j++) for(int i=0;i<tw;i++) {
        int sx=std::clamp(x+i-pad,0,w-1),sy=std::clamp(y+j-pad,0,h-1);
        const auto* px=static_cast<unsigned char*>(src.ptr)+sy*src.info.stride+sx*4;
        for(int c=0;c<3;c++) rgb[(j*tw+i)*3+c]=px[c];
    }
    ncnn::Mat in=ncnn::Mat::from_pixels(rgb.data(),ncnn::Mat::PIXEL_RGB,tw,th);
    const float norm[3]={1.f/255,1.f/255,1.f/255};
    in.substract_mean_normalize(nullptr,norm);
    auto ex=e->net.create_extractor();
    ncnn::Mat out;
    if(ex.input("data",in)!=0 || ex.extract("output",out)!=0 || out.empty() || out.w!=tw*4 || out.h!=th*4 || out.c!=3) {
        fail(env,"RealESRGAN inference failed. No resized substitute was used.");return;
    }
    for(int j=0;j<ch*4;j++) for(int i=0;i<cw*4;i++) {
        auto* px=static_cast<unsigned char*>(dst.ptr)+(y*4+j)*dst.info.stride+(x*4+i)*4;
        for(int c=0;c<3;c++) {
            float value=out.channel(c).row(j+pad*4)[i+pad*4];
            if(!std::isfinite(value)) { fail(env,"AI produced invalid pixels");return; }
            px[c]=static_cast<unsigned char>(std::clamp(int(std::round(value*255.f)),0,255));
        }
        // Preserve alpha independently; the photo model processes RGB only.
        const auto* alpha=static_cast<unsigned char*>(src.ptr)+(y+j/4)*src.info.stride+(x+i/4)*4;
        px[3]=alpha[3];
    }
    } catch (const std::bad_alloc&) { fail(env,"Not enough native memory for AI tile"); }
      catch (...) { fail(env,"Native AI tile failed"); }
}
