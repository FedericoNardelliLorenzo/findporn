package com.proyectos.findporn.iaAdapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyectos.findporn.dom.Fotograma;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

@Service("geminiAdapter")
public class GeminiAdapter implements IIAAdapter {

    // --- LLAVE SECRETA ---
    @Value("${google.gemini.api.key}")
    private String apiKey;

    // --- PROMPTS INYECTADOS ---
    @Value("${app.ia.prompts.resumen-fotogramas}")
    private String promptBaseResumen;

    @Value("${app.ia.prompts.frame}")
    private String promptFrame;

    @Value("${app.ia.prompts.audio}")
    private String promptAudio;

    @Value("${app.ia.prompts.video-completo}")
    private String promptVideoCompleto;

    // --- URLs INYECTADAS ---
    @Value("${app.ia.string.url-fotogramas}")
    private String urlFotogramas;

    @Value("${app.ia.string.url-frame}")
    private String urlFrame;

    @Value("${app.ia.string.url-audio}")
    private String urlAudio;

    @Value("${app.ia.string.url-videocompleto}")
    private String urlVideoCompleto;


    @Override
    public Fotograma procesarFrame(String rutaFrame, int numeroFrame) {
        try {
            // 1. Agarramos la foto física del disco duro y la leemos como bytes
            byte[] bytesDeLaFoto = Files.readAllBytes(Paths.get(rutaFrame));

            // 2. Transformamos esos bytes a un código de texto (Base64)
            String imagenBase64 = Base64.getEncoder().encodeToString(bytesDeLaFoto);

            // 3. Armamos el JSON especial Multimodal (Texto + Imagen) que pide Gemini
            String requestBody = "{\"contents\": [{\"parts\":[{\"text\": \"" + promptFrame + "\"}, {\"inline_data\": {\"mime_type\":\"image/jpeg\",\"data\":\"" + imagenBase64 + "\"}}]}]}";

            // 4. Preparamos el cartero y los headers
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            // 5. Armamos la URL exacta y disparamos la petición
            String url = urlFrame + apiKey;
            String respuestaJsonCruda = restTemplate.postForObject(url, request, String.class);

            // 6. Limpiamos la respuesta gigante y sacamos solo el texto útil
            ObjectMapper mapper = new ObjectMapper();
            JsonNode nodoRaiz = mapper.readTree(respuestaJsonCruda);
            String textoFinal = nodoRaiz
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText()
                    .replace("\n", "").trim(); // Le sacamos los saltos de línea basura

            // 7. ¡Magia! Devolvemos tu objeto Fotograma ya instanciado y listo
            return new Fotograma(numeroFrame, textoFinal);

        } catch (Exception e) {
            throw new RuntimeException("Error al procesar el fotograma del segundo " + numeroFrame + ": " + e.getMessage());
        }
    }

    @Override
    public String procesarFotogramas(List<Fotograma> fotogramas) {

        StringBuilder prompt = new StringBuilder(promptBaseResumen);
        for (Fotograma f : fotogramas) {
            prompt.append("- Segundo ").append(f.getSegundo()).append(": ").append(f.getDescripcion()).append("\n");
        }

        String textoLimpio = prompt.toString().replace("\"", "'").replace("\n", " ");
        String requestBody = "{\"contents\": [{\"parts\":[{\"text\": \"" + textoLimpio + "\"}]}]}";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        String url = urlFotogramas + apiKey;

        try {
            String respuestaJsonCruda = restTemplate.postForObject(url, request, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode nodoRaiz = mapper.readTree(respuestaJsonCruda);

            String textoFinal = nodoRaiz
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText();

            return textoFinal;

        } catch (Exception e) {
            throw new RuntimeException("Error al comunicarse con Gemini o al limpiar el JSON: " + e.getMessage());
        }
    }

    @Override
    public String procesarAudio(String rutaAudio) {
        try {
            // 1. Leemos el archivo .mp3 físico del disco duro
            byte[] bytesDelAudio = Files.readAllBytes(Paths.get(rutaAudio));

            // 2. Lo pasamos a Base64 (igual que hicimos con la foto)
            String audioBase64 = Base64.getEncoder().encodeToString(bytesDelAudio);

            // 3. Armamos el JSON multimodal. Fijate que el mime_type ahora es "audio/mp3"
            String requestBody = "{\"contents\": [{\"parts\":[{\"text\": \"" + promptAudio + "\"}, {\"inline_data\": {\"mime_type\":\"audio/mp3\",\"data\":\"" + audioBase64 + "\"}}]}]}";

            // 4. Preparamos el cartero de Spring Boot
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            // 5. Disparamos a la URL (usando la variable de audio que ya creaste)
            String url = urlAudio + apiKey;
            String respuestaJsonCruda = restTemplate.postForObject(url, request, String.class);

            // 6. Extraemos únicamente el texto de la transcripción/análisis
            ObjectMapper mapper = new ObjectMapper();
            JsonNode nodoRaiz = mapper.readTree(respuestaJsonCruda);

            String textoFinal = nodoRaiz
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText()
                    .replace("\n", " ").trim(); // Limpiamos un poco el formato

            // 7. Devolvemos el texto con todo lo que se habló o sonó en el video
            return textoFinal;

        } catch (Exception e) {
            throw new RuntimeException("Error crítico al procesar la pista de audio: " + e.getMessage());
        }
    }

    @Override
    public String procesarVideoCompleto(String descripcionFotogramas, String descripcionAudio) {

        // 1. Ensamblamos el "Súper Prompt" combinando ambas fuentes de información
        StringBuilder prompt = new StringBuilder(promptVideoCompleto).append("\n\n");
        prompt.append("--- ANÁLISIS VISUAL ---\n").append(descripcionFotogramas).append("\n\n");
        prompt.append("--- ANÁLISIS DE AUDIO ---\n").append(descripcionAudio).append("\n");

        // 2. Limpiamos comillas y saltos de línea para que el JSON no explote
        String textoLimpio = prompt.toString().replace("\"", "'").replace("\n", " ");

        // 3. Armamos la estructura JSON estándar para texto (como en procesarFotogramas)
        String requestBody = "{\"contents\": [{\"parts\":[{\"text\": \"" + textoLimpio + "\"}]}]}";

        // 4. Preparamos el cartero y los headers
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        // 5. Armamos la URL y disparamos la petición
        String url = urlVideoCompleto + apiKey;

        try {
            String respuestaJsonCruda = restTemplate.postForObject(url, request, String.class);

            // 6. Navegamos por el JSON de Google y extraemos el texto final
            ObjectMapper mapper = new ObjectMapper();
            JsonNode nodoRaiz = mapper.readTree(respuestaJsonCruda);

            String textoFinal = nodoRaiz
                    .path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text")
                    .asText();

            // 7. Devolvemos el veredicto definitivo de tu sistema
            return textoFinal;

        } catch (Exception e) {
            throw new RuntimeException("Error crítico al procesar la conclusión final del video: " + e.getMessage());
        }
    }
}