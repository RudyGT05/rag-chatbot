# =============================================================================
# Arma el ZIP que se le entrega al proyecto que usa el chatbot como libreria.
#
#   .\entrega-libreria\empaquetar.ps1        (desde la raiz del repo)
#
# Resultado: chatbot-service\target\chatbot-rag-1.0.0.zip
# (target/ esta ignorado por git: el zip nunca se sube al repo)
# =============================================================================
$ErrorActionPreference = "Stop"
$raiz     = Split-Path -Parent $PSScriptRoot
$servicio = Join-Path $raiz "chatbot-service"
$version  = "1.0.0"

Write-Host "1/3 Compilando con Maven (Docker)..."
docker run --rm -v "${servicio}:/app" -w /app maven:3.9-eclipse-temurin-17 mvn -q -B package -DskipTests
if ($LASTEXITCODE -ne 0) { throw "Fallo la compilacion" }

$jar = Join-Path $servicio "target\chatbot-rag-$version.jar"
if (-not (Test-Path $jar)) { throw "No se genero $jar" }

Write-Host "2/3 Juntando archivos..."
$carpeta = Join-Path $servicio "target\entrega"
if (Test-Path $carpeta) { Remove-Item $carpeta -Recurse -Force }
New-Item -ItemType Directory $carpeta | Out-Null
Copy-Item $jar                                              $carpeta
Copy-Item (Join-Path $servicio "pom.xml")                   $carpeta
Copy-Item (Join-Path $PSScriptRoot "INTEGRACION.md")        $carpeta
Copy-Item (Join-Path $PSScriptRoot "docker-compose.infra.yml") $carpeta
Copy-Item (Join-Path $PSScriptRoot "INTEGRACION-WEB.md")    $carpeta

# Parte web (Next.js): widget + BFF, con la misma estructura de carpetas
$web = Join-Path $raiz "web-publica"
foreach ($f in @("components\chat\ChatWidget.tsx", "components\chat\ChatWidget.module.css",
                 "app\api\chat\route.ts", "lib\servicios.ts", "lib\ip-cliente.ts")) {
    $destino = Join-Path $carpeta "web\$f"
    New-Item -ItemType Directory (Split-Path -Parent $destino) -Force | Out-Null
    Copy-Item (Join-Path $web $f) $destino
}

Write-Host "3/3 Comprimiendo..."
$zip = Join-Path $servicio "target\chatbot-rag-$version.zip"
if (Test-Path $zip) { Remove-Item $zip -Force }
# tar.exe (incluido en Windows 10+) en vez de Compress-Archive: Compress-Archive de
# PowerShell 5.1 guarda las rutas con "\" y en Mac/Linux salen nombres raros.
$contenido = (Get-ChildItem $carpeta).Name
tar.exe -a -c -f $zip -C $carpeta @contenido
if ($LASTEXITCODE -ne 0) { throw "Fallo al comprimir" }

Write-Host ""
Write-Host "Listo: $zip"
Get-ChildItem $carpeta -Recurse -File | Format-Table FullName, Length -AutoSize
