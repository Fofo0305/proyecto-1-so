package Sistema;
import Modelo.EventoSistema;
import java.util.concurrent.Semaphore;
/** Anillo acotado por computador; no adquiere bloqueos de otros nucleos. */
public final class RegistroSistema {
 private final EventoSistema[] datos=new EventoSistema[1024]; private int inicio,tamano; private long secuencia;
 private final Semaphore mutex=new Semaphore(1);
 public void agregar(long ciclo,int c,long p,String tipo,String detalle) { mutex.acquireUninterruptibly();try {
 EventoSistema e=new EventoSistema(++secuencia,ciclo,c,p,tipo,detalle);
 if(tamano<datos.length) datos[(inicio+tamano++)%datos.length]=e; else {datos[inicio]=e;inicio=(inicio+1)%datos.length;}
 } finally {mutex.release();} }
 public EventoSistema[] consultar(){mutex.acquireUninterruptibly();try{EventoSistema[] a=new EventoSistema[tamano];for(int i=0;i<tamano;i++)a[i]=datos[(inicio+i)%datos.length];return a;}finally{mutex.release();}}
}
