# Paquete de fuentes, sin artefactos compilados ni configuracion privada.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$outputDirectory = Join-Path $projectRoot 'release'
New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
$archivePath = Join-Path $outputDirectory ('maestranza-erp-source-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.zip')
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
# Lista explicita: nunca recorrer .env, target, node_modules, evidence/local o .git.
$roots = @('.github','docs','scripts','backend/src','backend/.mvn','frontend/src','frontend/public')
$files = @('README.md','.env.example','.gitignore','.gitattributes','compose.yaml',
 'backend/pom.xml','backend/mvnw','backend/mvnw.cmd','frontend/package.json',
 'frontend/package-lock.json','frontend/angular.json','frontend/tsconfig.json',
 'frontend/tsconfig.app.json','frontend/tsconfig.spec.json','frontend/proxy.conf.json')
$selected = @($files | ForEach-Object { Get-Item -LiteralPath (Join-Path $projectRoot $_) })
foreach ($folder in $roots) {
 $selected += Get-ChildItem -LiteralPath (Join-Path $projectRoot $folder) -File -Recurse
}
$selected += Get-ChildItem -LiteralPath (Join-Path $projectRoot 'evidence') -Filter '*.png' -File
$archive = [IO.Compression.ZipFile]::Open($archivePath,[IO.Compression.ZipArchiveMode]::Create)
try {
 foreach ($file in $selected) {
  if ($file.Attributes -band [IO.FileAttributes]::ReparsePoint) {throw 'No se permiten enlaces en el paquete.'}
  $relative = $file.FullName.Substring($projectRoot.Length + 1).Replace('\','/')
  if ($relative -match '(^|/)(\.env|node_modules|target|release|\.git)(/|$)' -or $relative -match '\.(log|pem|key|pfx)$') {
   throw "Archivo no permitido en el paquete: $relative"
  }
  [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive,$file.FullName,'maestranza-erp/'+$relative,[IO.Compression.CompressionLevel]::Optimal) | Out-Null
 }
} finally {$archive.Dispose()}
$hash=(Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant()
Set-Content -LiteralPath ($archivePath+'.sha256') -Value ($hash+'  '+[IO.Path]::GetFileName($archivePath)) -Encoding ascii
Write-Output $archivePath
Write-Output ('SHA256: '+$hash)
