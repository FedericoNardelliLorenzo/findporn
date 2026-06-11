package com.proyectos.findporn.services;

import com.proyectos.findporn.dom.Fotograma;
import com.proyectos.findporn.iaAdapters.IIAAdapter;
import org.jcodec.audio.Audio;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class FotogramasService {

    private final IIAAdapter iaFrame;

    private final IIAAdapter iaListaDeFrames;

    private final IIAAdapter iaFotogramasAudio;

    private final AudioService audioService;

    public FotogramasService(
            @Qualifier("geminiAdapter") IIAAdapter iaFrame,
            @Qualifier("geminiAdapter") IIAAdapter iaListaDeFrames,
            @Qualifier("geminiAdapter") IIAAdapter iaFotogramasAudio,
            AudioService audioService) {
        this.iaFrame = iaFrame;
        this.iaListaDeFrames = iaListaDeFrames;
        this.iaFotogramasAudio = iaFotogramasAudio;
        this.audioService = audioService;
    }

    @Value("${app.storage.frames.ruta}")
    private String carpetaFrames;

    public String recibirRuta(String ruta){
        List<String> rutasFrames = dividirVideo(ruta);

        String rutaAudio = audioService.extraerAudio(ruta);

        List<Fotograma> fotogramasAnalizados = new ArrayList<>();

        for (int i = 0; i < rutasFrames.size(); i++) {
            String rutaFisica = rutasFrames.get(i);

            Fotograma frameListo = this.procesarFrame(rutaFisica, i);

            fotogramasAnalizados.add(frameListo);
        }

        String descripcionTotalFotogramas = this.procesarFotogramas(fotogramasAnalizados);

        String descripcionAudio = this.procesarAudio(rutaAudio);

        return this.procesarDescripcionFotogramasAudio(descripcionTotalFotogramas,descripcionAudio);
    }

    public List<String> dividirVideo(String rutaVideo){
        // SE divide el video de la ruta (1 frame por segundo) y se lo guarda en carpetaFrames
        List<String> rutasDeLosFrames = new ArrayList<>();

        File archivoVideo = new File(rutaVideo);

        File directorioFrames = new File(carpetaFrames);

        if (!directorioFrames.exists()) {
            directorioFrames.mkdirs();
        }
        try {
            int segundo = 0;

            while (true) {

                org.jcodec.common.model.Picture frameCrudo = org.jcodec.api.FrameGrab.getFrameAtSec(archivoVideo, segundo);

                if (frameCrudo == null) {
                    break;
                }

                java.awt.image.BufferedImage imagenJava = org.jcodec.scale.AWTUtil.toBufferedImage(frameCrudo);

                String rutaDestino = carpetaFrames + "frame_" + segundo + ".jpg";
                File archivoFoto = new File(rutaDestino);

                javax.imageio.ImageIO.write(imagenJava, "jpg", archivoFoto);

                rutasDeLosFrames.add(rutaDestino);

                segundo++;
            }

        } catch (Exception e) {
            throw new RuntimeException("Fallo crítico al intentar dividir el video por fotogramas", e);
        }

        return rutasDeLosFrames;
    }

    public String procesarFotogramas(List<Fotograma> fotogramas){
        return iaListaDeFrames.procesarFotogramas(fotogramas);
    }

    public Fotograma procesarFrame(String rutaFrame,int numeroFrame){
        return iaFrame.procesarFrame(rutaFrame,numeroFrame);
    }

    public String procesarAudio(String rutaAudio){
        return audioService.procesarAudio(rutaAudio);
    }

    public String procesarDescripcionFotogramasAudio(String descripcionTotalFotogramas,String descripcionAudio){
        return iaFotogramasAudio.procesarVideoCompleto(descripcionTotalFotogramas,descripcionAudio);
    }


}
