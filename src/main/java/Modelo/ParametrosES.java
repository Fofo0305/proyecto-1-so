package Modelo;
/** Rafaga de CPU y duracion de una espera de E/S; ciclos enteros positivos. */
public record ParametrosES(int rafaga, int duracion) {
 public ParametrosES { if(rafaga<1 || duracion<1) throw new IllegalArgumentException("Rafaga y E/S deben ser positivas"); }
}
