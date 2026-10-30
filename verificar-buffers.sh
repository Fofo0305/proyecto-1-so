#!/bin/sh
# Compila el proyecto y corre las comprobaciones de src/test/java/Pruebas.
# Equivalente de verificar-buffers.ps1 para macOS y Linux.
#
#   ./verificar-buffers.sh [--jdk RUTA] [--interfaz]
#
# Sin --interfaz corre las 16 comprobaciones que no necesitan pantalla.
# Con --interfaz agrega las 3 que crean ventanas Swing; en un servidor sin
# escritorio hay que envolverlas, por ejemplo con `xvfb-run -a`.
set -eu

JDK="${JAVA_HOME:-}"
INTERFAZ=0
while [ $# -gt 0 ]; do
    case "$1" in
        --jdk) JDK="${2:?--jdk necesita una ruta}"; shift 2 ;;
        --interfaz) INTERFAZ=1; shift ;;
        -h|--ayuda) echo "Uso: $0 [--jdk RUTA] [--interfaz]"; exit 0 ;;
        *) echo "Argumento desconocido: $1" >&2; exit 2 ;;
    esac
done
if [ -z "$JDK" ]; then
    echo "Indique --jdk con la ruta de un JDK 22 o posterior, o configure JAVA_HOME." >&2
    exit 1
fi

COMPILADOR="$JDK/bin/javac"
JAVA="$JDK/bin/java"
cd "$(dirname "$0")"
SALIDA=target/pruebas-buffers
mkdir -p "$SALIDA"

find src/main/java src/test/java -name '*.java' > "$SALIDA/fuentes.txt"
"$COMPILADOR" -encoding UTF-8 --release 22 -d "$SALIDA" "@$SALIDA/fuentes.txt"

PRUEBAS="Pruebas.PruebaBuffer
Pruebas.PruebaRegistroBuffers
Pruebas.PruebaPanelBuffer
Pruebas.PruebaSemaforos
Pruebas.PruebaBloqueoSemaforos
Pruebas.PruebaProductorConsumidorLocal
Pruebas.PruebaAccesoRemoto
Pruebas.PruebaLimpiezaProductorConsumidor
Pruebas.PruebaEventosBuffers
Pruebas.PruebaContadoresBuffers
Pruebas.PruebaTiemposBloqueo
Pruebas.PruebaObservacionIntegrada
Pruebas.PruebaPanelLatencia
Pruebas.PruebaPoliticasES
Pruebas.PruebaPanelPlanificacion
Pruebas.PruebaRelojPoliticas"

if [ "$INTERFAZ" -eq 1 ]; then
    PRUEBAS="$PRUEBAS
Pruebas.PruebaPanelObservacion
Pruebas.PruebaVentanaBuffers
Pruebas.PruebaVentanaProductorConsumidor"
fi

CANTIDAD=0
for PRUEBA in $PRUEBAS; do
    printf '== %s\n' "$PRUEBA"
    if ! "$JAVA" -cp "$SALIDA" "$PRUEBA"; then
        echo "Fallo $PRUEBA" >&2
        exit 1
    fi
    CANTIDAD=$((CANTIDAD + 1))
done
printf '\n%s comprobaciones pasaron.\n' "$CANTIDAD"
