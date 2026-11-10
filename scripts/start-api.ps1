param([switch]$ConfigureAdmin)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$jar = Join-Path $projectRoot 'backend/target/erp-0.0.1-SNAPSHOT.jar'
if (!(Test-Path $jar)) { throw 'Primero compila el backend siguiendo docs/runbook.md.' }
if (Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue) {
    throw 'La API ya esta abierta. Detenla antes de iniciar otra instancia.'
}
$java = 'C:/Program Files/Eclipse Adoptium/jdk-21.0.12.8-hotspot/bin/java.exe'
if (!(Test-Path $java)) { $java = (Get-Command java -ErrorAction Stop).Source }
$previousDbPassword = $env:DB_PASSWORD
$previousAdminPassword = $env:ERP_ADMIN_PASSWORD
try {
    $env:DB_PASSWORD = (Get-Content (Join-Path $projectRoot '.env') | Where-Object { $_ -match '^DB_PASSWORD=' }) -replace '^DB_PASSWORD=', ''
    if (!$env:DB_PASSWORD) { throw 'Falta DB_PASSWORD en .env.' }
    $env:ERP_ADMIN_PASSWORD = $null
    if ($ConfigureAdmin) {
        $securePassword = Read-Host 'Contrasena inicial para admin (minimo 12 caracteres)' -AsSecureString
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
        try {
            $env:ERP_ADMIN_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
            if ($env:ERP_ADMIN_PASSWORD.Length -lt 12 -or [Text.Encoding]::UTF8.GetByteCount($env:ERP_ADMIN_PASSWORD) -gt 72) {
                throw 'Usa al menos 12 caracteres y no mas de 72 bytes UTF-8.'
            }
        } finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
            $securePassword.Dispose()
        }
    }
    & $java -jar $jar
} finally {
    $env:DB_PASSWORD = $previousDbPassword
    $env:ERP_ADMIN_PASSWORD = $previousAdminPassword
}

