package com.proyectos.findporn.iaAdapters;

import com.proyectos.findporn.dom.Fotograma;

import java.util.List;

public interface IIAAdapter {

    Fotograma procesarFrame(String rutaFrame,int numeroFrame);                           //Metodo para la IA #1

    String procesarFotogramas(List<Fotograma> fotogramas);                               //Metodo para la IA #2

    String procesarAudio(String rutaAudio);                                              //Metodo para la IA #3

    String procesarVideoCompleto(String descripcionFotogramas, String descripcionAudio); //Metodo para la IA  #4

}
