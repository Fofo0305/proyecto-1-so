package Pruebas;

import Modelo.*;
import Sistema.ControladorSimulacion;
import static Pruebas.PruebaSemaforos.verificar;

/** Observacion integrada con vencimientos y cancelacion en distintos ciclos.
 * @author Luciano Rojas */
public final class PruebaObservacionIntegrada {
    public static void main(String[] args) {
        for (int equipo : new int[]{1,2}) {
            for (int limite=1;limite<=18;limite++) comprobar(equipo,limite,false);
            for (int limite=1;limite<=18;limite++) comprobar(equipo,limite,true);
        }
        System.out.println("OK: 72 escenarios de observacion local/remota, vencimientos y cancelacion sin duplicados");
    }
    private static void comprobar(int equipo,int limite,boolean cancelar) {
        try (ControladorSimulacion c = new ControladorSimulacion()) {
            c.configurar(new Configuracion(new int[]{32,32},1,4,3));
            c.crearBuffer("B",2,1);
            Proceso productor=c.crearProcesoBuffer("P",equipo,0,2,cancelar?100:limite,TipoProceso.PRODUCTOR,"B",1,3);
            c.crearProcesoBuffer("C",equipo==1?2:1,0,2,80,TipoProceso.CONSUMIDOR,"B",1,3);
            VistaObservacionBuffers anterior=c.consultarObservacionBuffers();
            for (int ciclo=1;ciclo<=90;ciclo++) {
                c.avanzarCiclo();
                if (cancelar && ciclo==limite) {
                    c.cancelarProceso(productor);
                    verificar(!c.cancelarProceso(productor),"La cancelacion se repite");
                }
                VistaObservacionBuffers vista=c.consultarObservacionBuffers();
                comprobarVista(vista);
                VistaObservacionBuffers repetida=c.consultarObservacionBuffers();
                verificar(vista.contadores()[0].equals(repetida.contadores()[0])
                        && vista.bloqueos()[0].equals(repetida.bloqueos()[0])
                        && vista.eventos()[0].eventos().length==repetida.eventos()[0].eventos().length,
                        "Observar modifica la ejecucion");
                verificar(anterior.contadores()[0].producidos()==0 && anterior.eventos()[0].eventos().length==0,
                        "Una copia antigua cambio durante la ejecucion");
            }
            VistaObservacionBuffers fin=c.consultarObservacionBuffers();
            verificar(fin.bloqueos()[0].abiertasSemaforo()==0 && fin.bloqueos()[0].abiertasRed()==0,
                    "La limpieza dejo esperas abiertas");
            c.configurar(new Configuracion(new int[]{32,32},1,4,3));
            verificar(c.consultarObservacionBuffers().estados().length==0,"El reinicio conserva buffers de otra sesion");
            comprobarVista(fin);
        }
    }
    private static void comprobarVista(VistaObservacionBuffers vista) {
        VistaContadoresBuffer b=vista.contadores()[0];
        verificar(b.producidos()-b.consumidos()==b.cantidad(),"Ocupacion incoherente");
        long producciones=0,consumos=0,secuencia=0,ciclo=-1;
        verificar(vista.eventos()[0].descartados()==0,"El escenario excede el historial previsto");
        for (EventoBuffer evento:vista.eventos()[0].eventos()) {
            verificar(evento.secuencia()>secuencia && evento.ciclo()>=ciclo,"Eventos fuera de orden");
            secuencia=evento.secuencia(); ciclo=evento.ciclo();
            if (evento.tipo()==TipoEventoBuffer.PRODUCCION) producciones++;
            if (evento.tipo()==TipoEventoBuffer.CONSUMO) consumos++;
        }
        verificar(producciones==b.producidos() && consumos==b.consumidos(),"Evento duplicado o modificacion sin registrar");
        long sem=0,red=0,esperasSem=0,esperasRed=0,abiertasSem=0,abiertasRed=0;
        for (VistaBloqueoProceso p:vista.procesos()) {
            sem+=p.ciclosSemaforo(); red+=p.ciclosRed(); esperasSem+=p.esperasSemaforo(); esperasRed+=p.esperasRed();
            if (p.esperaActual()==MotivoBloqueo.SEMAFORO) abiertasSem++;
            if (p.esperaActual()==MotivoBloqueo.RED) abiertasRed++;
        }
        VistaBloqueosBuffer m=vista.bloqueos()[0];
        verificar(sem==m.ciclosSemaforo() && red==m.ciclosRed() && esperasSem==m.esperasSemaforo()
                && esperasRed==m.esperasRed() && abiertasSem==m.abiertasSemaforo() && abiertasRed==m.abiertasRed(),
                "Los perfiles no concuerdan con el buffer en la barrera");
    }
}