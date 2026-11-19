package Sistema;

import Modelo.AreaDisco;
import Modelo.VistaBloqueDisco;

/** Estado mutable de un bloque, encapsulado por Disco. @author Adolfo */
final class BloqueDisco {
    private final int numero;
    private final AreaDisco area;
    private String propietario;
    private int siguiente = -1;

    BloqueDisco(int numero, AreaDisco area) {
        this.numero = numero;
        this.area = area;
    }

    void ocupar(String nuevoPropietario, int proximo) {
        if (nuevoPropietario == null || nuevoPropietario.isBlank() || propietario != null
                || proximo < -1 || proximo == numero)
            throw new IllegalStateException("El bloque no puede ocuparse con esos datos");
        propietario = nuevoPropietario;
        siguiente = proximo;
    }

    void liberar() {
        propietario = null;
        siguiente = -1;
    }

    boolean estaLibre() { return propietario == null; }
    AreaDisco getArea() { return area; }
    VistaBloqueDisco consultar() { return new VistaBloqueDisco(numero, area, propietario, siguiente); }
}
