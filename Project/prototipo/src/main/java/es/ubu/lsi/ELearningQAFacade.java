package es.ubu.lsi;

import com.fasterxml.jackson.databind.ObjectMapper;
import es.ubu.lsi.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Map;

public class ELearningQAFacade {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int CHECKS_DISENO =6;
    private static final int CHECKS_IMPLEMENTACION =5;
    private static final int CHECKS_REALIZACION =4;
    private static final int CHECKS_EVALUACION =2;
    private static final int CHECKS_TOTAL = CHECKS_DISENO + CHECKS_IMPLEMENTACION + CHECKS_REALIZACION + CHECKS_EVALUACION;
    protected static String[] camposInformeFases;
    private final FacadeConfig config;

    static {
        String sep= File.separator;
        String ruta="."+sep+"src"+sep+"main"+sep+"resources"+sep+"json"+sep+"informe"+sep;
        String extension=".json";
        ObjectMapper mapper=new ObjectMapper();
        try {
            camposInformeFases= mapper.readValue(new File(ruta+"CamposInformeFases"+extension), String[].class);
        } catch (Exception e) {
            LOGGER.error("exception", e);
        }
    }

    public ELearningQAFacade(String configFile, String host) {
        config=new FacadeConfig(configFile);
        config.setHost(host);
    }

    public String conectarse(String username, String password){
        return WebServiceClient.login(username, password, config.getHost());
    }

    public String generarListaCursos(String token){
        List<Course> listaCursos= getListaCursos(token);
        StringBuilder listaEnTabla= new StringBuilder("<table>");
        for (Course curso: listaCursos) {
            listaEnTabla.append("<tr><td><a target=\"_blank\" href=\"./informe?courseid=").append(curso.getId())
                    .append("\">").append(curso.getFullname())
                    .append(" ("+curso.getCoursecategory()+")").append("</a></td></tr>");
        }
        listaEnTabla.append("</table>");
        return listaEnTabla.toString();
    }

    public List<Course> getListaCursos(String token) {
        return WebServiceClient.obtenerCursos(token, config.getHost());
    }

    public String obtenerNombreCompleto(String token, String username) {
        return WebServiceClient.obtenerNombreCompleto(token, username, config.getHost());
    }

    public int[] realizarComprobaciones(String token, long courseid, AlertLog registro) {
        Course curso= getCursoPorId(token, courseid);
        List<User> listaUsuarios= WebServiceClient.obtenerUsuarios(token, courseid, config.getHost());
        StatusList listaEstados=WebServiceClient.obtenerListaEstados(token, courseid, listaUsuarios, config.getHost());
        List<es.ubu.lsi.model.Module> listaModulos=WebServiceClient.obtenerListaModulos(token, courseid, config.getHost());
        List<Group> listaGrupos=WebServiceClient.obtenerListaGrupos(token, courseid, config.getHost(), registro);
        List<Assignment> listaTareas=WebServiceClient.obtenerListaTareas(token, courseid, config.getHost());
        List<Table> listaCalificadores=WebServiceClient.obtenerCalificadores(token, courseid, config.getHost());
        List<Resource> listaRecursos=WebServiceClient.obtenerRecursos(token, courseid, config.getHost());
        List<CourseModule> listaModulosTareas=WebServiceClient.obtenerModulosTareas(token, listaTareas, config.getHost());
        List<Post> listaPosts=WebServiceClient.obtenerListaPosts(token, courseid, config.getHost());
        Map<Integer, Long> mapaFechasLimite=WebServiceClient.generarMapaFechasLimite(listaTareas);
        List<Assignment> tareasConNotas=WebServiceClient.obtenerTareasConNotas(token,mapaFechasLimite, config.getHost(), listaTareas);
        List<ResponseAnalysis> listaAnalisis=WebServiceClient.obtenerAnalisis(token, courseid, config.getHost());
        List<Survey> listaSurveys=WebServiceClient.obtenerSurveys(token, courseid, config.getHost());
        List<es.ubu.lsi.model.Module> modulosMalFechados=WebServiceClient.obtenerModulosMalFechados(curso, listaModulos);
        List<Resource> recursosDesfasados=WebServiceClient.obtenerRecursosDesfasados(curso, listaRecursos);
        int[] puntosComprobaciones = new int[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        if(isestaProgresoActivado(listaEstados, registro)){puntosComprobaciones[0]++;}
        if(isHayVariedadFormatos(listaModulos, registro)){puntosComprobaciones[1]++;}
        if(isTieneGrupos(listaGrupos, registro)){puntosComprobaciones[2]++;}
        if(isHayTareasGrupales(listaTareas, registro)){puntosComprobaciones[3]++;}
        if(isSonVisiblesCondiciones(curso, registro)){puntosComprobaciones[4]++;}
        if(isEsNotaMaxConsistente(listaCalificadores, registro)){puntosComprobaciones[5]++;}
        if(isEstanActualizadosRecursos(recursosDesfasados, registro)){puntosComprobaciones[6]++;}
        if(isSonFechasCorrectas(modulosMalFechados, registro)){puntosComprobaciones[7]++;}
        if(isMuestraCriterios(listaModulosTareas, registro)){puntosComprobaciones[8]++;}
        if(isAnidamientoCalificadorAceptable(listaCalificadores, registro)){puntosComprobaciones[9]++;}
        if(isAlumnosEnGrupos(listaUsuarios, registro)){puntosComprobaciones[10]++;}
        if(isRespondeATiempo(listaUsuarios,listaPosts, registro)){puntosComprobaciones[11]++;}
        if(isHayRetroalimentacion(listaCalificadores, registro)){puntosComprobaciones[12]++;}
        if(isEstaCorregidoATiempo(tareasConNotas,listaUsuarios, registro)){puntosComprobaciones[13]++;}
        if(isCalificadorMuestraPonderacion(listaCalificadores, registro)){puntosComprobaciones[14]++;}
        if(isRespondenFeedbacks(listaAnalisis,listaUsuarios, registro)){puntosComprobaciones[15]++;}
        if(isUsaSurveys(listaSurveys, registro)){puntosComprobaciones[16]++;}
        return puntosComprobaciones;
    }

    public String generarInformeFases(int[] puntos, int nroCursos) {
        int contadorDiseno=puntos[0]+puntos[1]+puntos[2]+puntos[3]+puntos[4]+puntos[5];
        int contadorImplementacion=puntos[6]+puntos[7]+puntos[8]+puntos[9]+puntos[10];
        int contadorRealizacion=puntos[11]+puntos[12]+puntos[13]+puntos[14];
        int contadorEvaluacion=puntos[15]+puntos[16];
        int contadorTotal=contadorDiseno+contadorImplementacion+contadorRealizacion+contadorEvaluacion;
        return camposInformeFases[0]+generarCampoRelativo((float)contadorTotal/nroCursos, CHECKS_TOTAL) +
                camposInformeFases[1]+generarCampoRelativo((float)contadorDiseno/nroCursos, CHECKS_DISENO) +
                generarFilas(new int[]{2, 0}, 6, puntos, nroCursos)+
                camposInformeFases[8]+generarCampoRelativo((float)contadorImplementacion/nroCursos, CHECKS_IMPLEMENTACION) +
                generarFilas(new int[]{9, 6}, 5, puntos, nroCursos)+
                camposInformeFases[14]+generarCampoRelativo((float)contadorRealizacion/nroCursos, CHECKS_REALIZACION) +
                generarFilas(new int[]{15, 11}, 4, puntos, nroCursos)+
                camposInformeFases[19]+generarCampoRelativo((float)contadorEvaluacion/nroCursos, CHECKS_EVALUACION) +
                generarFilas(new int[]{20, 15}, 2, puntos, nroCursos)+camposInformeFases[22];
    }

    private String generarFilas(int[] posiciones, int cantidad, int[] puntos, int nroCursos){
        StringBuilder filas= new StringBuilder();
        if(nroCursos==1){
            for (int i = 0; i < cantidad; i++) {
                filas.append(camposInformeFases[posiciones[0] + i]).append(generarCampoAbsoluto(puntos[posiciones[1] + i]));
            }
        }else{
            for (int i = 0; i < cantidad; i++) {
                filas.append(camposInformeFases[posiciones[0] + i]).append(generarCampoRelativo(puntos[posiciones[1] + i],nroCursos));
            }
        }
        return filas.toString();
    }


    public boolean isSonVisiblesCondiciones(Course curso, AlertLog registro) {
        return WebServiceClient.sonVisiblesCondiciones(curso, registro);
    }

    public boolean isTieneGrupos(List<Group> listaGrupos, AlertLog registro) {
        return WebServiceClient.tieneGrupos(listaGrupos, registro);
    }

    public Course getCursoPorId(String token, long courseid) {
        return WebServiceClient.obtenerCursoPorId(token, courseid, config.getHost());
    }



    public boolean isestaProgresoActivado(StatusList listaEstados, AlertLog registro) {
        return WebServiceClient.estaProgresoActivado(listaEstados, registro);
    }

    public boolean isUsaSurveys(List<Survey> listaEncuestas, AlertLog registro) {
        return WebServiceClient.usaSurveys(listaEncuestas, registro);
    }

    public boolean isAlumnosEnGrupos(List<User> listaUsuarios, AlertLog registro) {
        return WebServiceClient.alumnosEnGrupos(listaUsuarios, registro);
    }

    public boolean isEstaCorregidoATiempo(List<Assignment> tareasConNotas, List<User> listaUsuarios, AlertLog registro) {
        return WebServiceClient.estaCorregidoATiempo(tareasConNotas, listaUsuarios, registro, config);
    }

    public boolean isRespondeATiempo(List<User> listaUsuarios, List<Post> listaPostsCompleta, AlertLog registro) {
        return WebServiceClient.respondeATiempo(listaUsuarios, listaPostsCompleta, registro, config);
    }

    public boolean isAnidamientoCalificadorAceptable(List<Table> listaCalificadores, AlertLog registro) {
        return WebServiceClient.anidamientoCalificadorAceptable(listaCalificadores, registro, config);
    }

    public boolean isCalificadorMuestraPonderacion(List<Table> listaCalificadores, AlertLog registro) {
        return WebServiceClient.calificadorMuestraPonderacion(listaCalificadores, registro);
    }

    public boolean isHayRetroalimentacion(List<Table> listaCalificadores, AlertLog registro) {
        return WebServiceClient.hayRetroalimentacion(listaCalificadores, registro, config);
    }

    public boolean isEsNotaMaxConsistente(List<Table> listaCalificadores, AlertLog registro) {
        return WebServiceClient.esNotaMaxConsistente(listaCalificadores, registro);
    }

    public boolean isEstanActualizadosRecursos(List<Resource> listaRecursosDesfasados, AlertLog registro) {
        return WebServiceClient.estanActualizadosRecursos(listaRecursosDesfasados, registro, config);
    }

    public boolean isSonFechasCorrectas(List<es.ubu.lsi.model.Module> listaModulosMalFechados, AlertLog registro) {
        return WebServiceClient.sonFechasCorrectas(listaModulosMalFechados, registro);
    }

    public boolean isRespondenFeedbacks(List<ResponseAnalysis> listaAnalisis, List<User> listaUsuarios, AlertLog registro) {
        return WebServiceClient.respondenFeedbacks(listaAnalisis,listaUsuarios, registro, config);
    }

    public boolean isHayTareasGrupales(List<Assignment> listaTareas, AlertLog registro) {
        return WebServiceClient.hayTareasGrupales(listaTareas, registro);
    }

    public boolean isMuestraCriterios(List<CourseModule> listaModulosTareas, AlertLog registro) {
        return WebServiceClient.muestraCriterios(listaModulosTareas, registro);
    }

    public boolean isHayVariedadFormatos(List<es.ubu.lsi.model.Module> listamodulos, AlertLog registro) {
        return WebServiceClient.hayVariedadFormatos(listamodulos, registro, config);
    }

    public float porcentajeFraccion(float numerador, float denominador){
        return numerador/denominador*100;
    }

    public String generarCampoAbsoluto(int puntos){
        if (puntos==0){
            return "<td class=\"tg-pred\"><img src=\"Cross.png\" width=\"16\" height=\"16\" alt=\"No\"></td>";
        }else{
            return "<td class=\"tg-pgre\"><img src=\"Check.png\" width=\"16\" height=\"16\" alt=\"Sí." +
                    "\"></td>";
        }
    }

    public String generarCampoRelativo(float numerador, float denominador){
        float resultado= numerador/denominador;
        String campoAMedias="<meter value=\""+numerador+"\" min=\"0\" max=\""+denominador+"\"></meter>"+
                String.format("%.1f",porcentajeFraccion(numerador, denominador))+"%"+"</td>";
        if (resultado<0.2){return "<td class=\"tg-pred\">"+campoAMedias;}
        if (resultado<0.4){return "<td class=\"tg-oran\">"+campoAMedias;}
        if (resultado<0.6){return "<td class=\"tg-yell\">"+campoAMedias;}
        if (resultado<0.8){return "<td class=\"tg-char\">"+campoAMedias;}
        else{return "<td class=\"tg-pgre\">"+campoAMedias;}
    }

    public float[] calcularPorcentajesMatriz(int[] puntos,int numeroCursos){
        int[][] matrizPuntos=new int[][]{
                {3,1,0,0,0,0,0,0,0},
                {3,1,1,3,1,1,0,0,0},
                {3,1,1,0,0,0,0,0,0},
                {3,1,1,0,0,0,0,0,0},
                {3,1,1,0,0,0,0,0,0},
                {3,1,1,0,0,0,0,0,0},
                {3,1,1,3,1,1,0,0,0},
                {1,3,1,1,3,1,0,0,0},
                {3,1,1,3,1,1,0,0,0},
                {3,1,1,0,0,0,3,1,1},
                {0,0,0,1,0,3,1,0,3},
                {1,3,1,1,3,1,0,0,0},
                {1,3,1,1,3,1,0,0,0},
                {1,3,1,1,3,1,0,0,0},
                {1,3,1,1,3,1,0,0,0},
                {1,1,3,1,1,3,1,1,3},
                {1,1,3,1,1,3,1,1,3}
        };
        float[] porcentajes=new float[9];
        int[] puntuacionesMax=new int[]{34*numeroCursos,26*numeroCursos,19*numeroCursos,17*numeroCursos,20*numeroCursos,17*numeroCursos,6*numeroCursos,3*numeroCursos,10*numeroCursos};
        for(int i=0;i<matrizPuntos.length;i++){
            for(int j=0;j<porcentajes.length;j++){
                porcentajes[j]+= (float) (matrizPuntos[i][j] * puntos[i]) /puntuacionesMax[j];
            }
        }
        return porcentajes;
    }

    public String generarMatrizRolPerspectiva(float[] porcentajes){
        return "<table class=\"tg\"><tr><td class=\"tg-plgr\"></td><td class=\"tg-plgr\">Diseñador</td>"+
                "<td class=\"tg-plgr\">Facilitador</td><td class=\"tg-plgr\">Proveedor</td>"+
                "</tr><tr><td class=\"tg-plgr\">Pedagógica</td>"+generarCampoRelativo(porcentajes[0],1)+
                generarCampoRelativo(porcentajes[1],1)+generarCampoRelativo(porcentajes[2],1)+
                "</tr><tr><td class=\"tg-plgr\">Tecnológica</td>"+generarCampoRelativo(porcentajes[3],1)+
                generarCampoRelativo(porcentajes[4],1)+generarCampoRelativo(porcentajes[5],1)+
                "</tr><tr><td class=\"tg-plgr\">Estratégica</td>"+generarCampoRelativo(porcentajes[6],1)+
                generarCampoRelativo(porcentajes[7],1)+generarCampoRelativo(porcentajes[8],1)+
                "</tr></table>";
    }

}
