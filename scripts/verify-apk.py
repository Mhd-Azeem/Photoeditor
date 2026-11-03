import hashlib
import json
import sys
import zipfile
from pathlib import Path
apk = Path(sys.argv[1])
models = dict(line.split()[::-1] for line in Path('scripts/model-sha256.txt').read_text().splitlines())
with zipfile.ZipFile(apk) as z:
    for name, sha in models.items():
        path = 'assets/models/' + name
        assert hashlib.sha256(z.read(path)).hexdigest() == sha, path
    libraries = []
    for abi in ('arm64-v8a', 'armeabi-v7a', 'x86_64'):
        path = f'lib/{abi}/libaz_upscaler.so'
        data = z.read(path)
        assert data[:4] == b'\x7fELF' and len(data) > 1000000, path
        assert b'Java_com_photoeditor_ai_OfflineUpscaler_nativeTile' in data, path
        libraries.append({'path': path, 'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest()})
report = {'apk': apk.name, 'bytes': apk.stat().st_size, 'sha256': hashlib.sha256(apk.read_bytes()).hexdigest(), 'models': models, 'native_libraries': libraries}
Path('release-apks/verification.json').write_text(json.dumps(report, indent=2))
print(json.dumps(report, indent=2))
