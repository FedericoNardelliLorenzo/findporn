package com.proyectos.findporn.services;

import com.proyectos.findporn.iaAdapters.IIAAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.Encoder;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;

@Service
public class AudioService {

    @Value("${app.storage.audios.ruta}")
    private String carpetaAudios;

    private final IIAAdapter iaAudio;

    public AudioService(@Qualifier("geminiAdapter") IIAAdapter iaAudio) {
        this.iaAudio = iaAudio;
    }

    public String extraerAudio(String rutaVideo){
        File archivoVideo = new File(rutaVideo);

        File directorioAudios = new File(carpetaAudios);
        if (!directorioAudios.exists()) {
            directorioAudios.mkdirs();
        }

        String nombreOriginal = archivoVideo.getName();
        String nombreAudio = nombreOriginal.replaceFirst("[.][^.]+$", "") + ".mp3";
        String rutaDestino = carpetaAudios + nombreAudio;
        File archivoDestino = new File(rutaDestino);

        try {
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libmp3lame");
            audio.setBitRate(128000);
            audio.setChannels(2);
            audio.setSamplingRate(44100);

            EncodingAttributes atributos = new EncodingAttributes();
            atributos.setOutputFormat("mp3");
            atributos.setAudioAttributes(audio);

            Encoder encoder = new Encoder();
            encoder.encode(new MultimediaObject(archivoVideo), archivoDestino, atributos);

            System.out.println("Audio extraído con éxito en: " + rutaDestino);

            return rutaDestino;

        } catch (Exception e) {
            throw new RuntimeException("Fallo crítico al extraer el audio del video", e);
        }
    }

    public String procesarAudio(String rutaAudio){
        return iaAudio.procesarAudio(rutaAudio);
    }
}
