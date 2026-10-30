param([string]$Jdk = $env:JAVA_HOME, [switch]$Interfaz)
$ErrorActionPreference = 'Stop'
if (-not $Jdk) { throw 'Indique -Jdk con la ruta de un JDK 22 o posterior, o configure JAVA_HOME.' }
$compilador = Join-Path $Jdk 'bin/javac.exe'
$java = Join-Path $Jdk 'bin/java.exe'
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Force target/pruebas-buffers | Out-Null
    $fuentesBuffers = @(Get-ChildItem src/main/java,src/test/java -Recurse -Filter *.java | ForEach-Object FullName)
    & $compilador -encoding UTF-8 --release 22 -d target/pruebas-buffers @fuentesBuffers
    if ($LASTEXITCODE -ne 0) { throw 'Fallo la compilacion.' }
    foreach ($pruebaBuffers in @('Pruebas.PruebaBuffer', 'Pruebas.PruebaRegistroBuffers', 'Pruebas.PruebaPanelBuffer',
            'Pruebas.PruebaSemaforos', 'Pruebas.PruebaBloqueoSemaforos', 'Pruebas.PruebaProductorConsumidorLocal',
            'Pruebas.PruebaAccesoRemoto', 'Pruebas.PruebaLimpiezaProductorConsumidor', 'Pruebas.PruebaEventosBuffers',
            'Pruebas.PruebaContadoresBuffers', 'Pruebas.PruebaTiemposBloqueo', 'Pruebas.PruebaObservacionIntegrada',
            'Pruebas.PruebaPanelLatencia', 'Pruebas.PruebaPoliticasES', 'Pruebas.PruebaPanelPlanificacion',
            'Pruebas.PruebaRelojPoliticas')) {
        & $java -cp target/pruebas-buffers $pruebaBuffers
        if ($LASTEXITCODE -ne 0) { throw "Fallo $pruebaBuffers" }
    }
    if ($Interfaz) {
        & $java -cp target/pruebas-buffers Pruebas.PruebaPanelObservacion
        if ($LASTEXITCODE -ne 0) { throw 'Fallo la observacion Swing de buffers.' }
        & $java -cp target/pruebas-buffers Pruebas.PruebaVentanaBuffers
        if ($LASTEXITCODE -ne 0) { throw 'Fallo la integracion de buffers con la ventana.' }
        & $java -cp target/pruebas-buffers Pruebas.PruebaVentanaProductorConsumidor
        if ($LASTEXITCODE -ne 0) { throw 'Fallo la integracion de productor consumidor con la ventana.' }
        & $java -cp target/pruebas-buffers Pruebas.PruebaFlujoCompleto
        if ($LASTEXITCODE -ne 0) { throw 'Fallo el flujo completo desde las acciones Swing.' }
        & $java -cp target/pruebas-buffers Pruebas.PruebaVentanaNormal
        if ($LASTEXITCODE -ne 0) { throw 'Fallo la ventana sin maximizar.' }
        & $java -cp target/pruebas-buffers Pruebas.PruebaScrollColas
        if ($LASTEXITCODE -ne 0) { throw 'Fallo la estabilidad del scroll de colas.' }
    }
} finally { Pop-Location }
