param([string]$Jdk = $env:JAVA_HOME, [switch]$Interfaz)
$ErrorActionPreference = 'Stop'
if (-not $Jdk) { throw 'Indique -Jdk con la ruta de un JDK 22 o posterior, o configure JAVA_HOME.' }
$compilador = Join-Path $Jdk 'bin/javac.exe'
$java = Join-Path $Jdk 'bin/java.exe'
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Force target/pruebas-proyecto2 | Out-Null
    # Gson es la libreria JSON permitida; el pom usa la misma version.
    $gson = 'lib/gson-2.11.0.jar'
    $classpath = "target/pruebas-proyecto2;$gson"
    $fuentesProyecto2 = @(Get-ChildItem src/main/java,src/test/java -Recurse -Filter *.java | ForEach-Object FullName)
    & $compilador -encoding UTF-8 --release 22 -cp $gson -d target/pruebas-proyecto2 @fuentesProyecto2
    if ($LASTEXITCODE -ne 0) { throw 'Fallo la compilacion.' }
    foreach ($pruebaProyecto2 in @('Pruebas.PruebaBaseProyecto2', 'Pruebas.PruebaModeloSieteEstados',
            'Pruebas.PruebaDiscoPlanificacion', 'Pruebas.PruebaPoliticasES',
            'Pruebas.PruebaDiscoReloj',
            'Pruebas.PruebaMetricasDisco',
            'Pruebas.PruebaRelojPoliticas',
            'Pruebas.PruebaSistemaArchivos')) {
        & $java -cp $classpath $pruebaProyecto2
        if ($LASTEXITCODE -ne 0) { throw "Fallo $pruebaProyecto2" }
    }
    if ($Interfaz) {
        & $java -cp $classpath Pruebas.PruebaPanelPlanificacion
        if ($LASTEXITCODE -ne 0) { throw 'Fallo el panel de planificacion.' }
        & $java -cp $classpath Pruebas.PruebaScrollColas
        if ($LASTEXITCODE -ne 0) { throw 'Fallo la estabilidad del scroll de colas.' }
    }
} finally { Pop-Location }
