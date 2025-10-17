package com.projects.wtg.service;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.projects.wtg.model.Address;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GeocodingService {

    private static final Logger logger = LoggerFactory.getLogger(GeocodingService.class);
    private final GeoApiContext context;
    // SRID 4326 é o padrão para WGS 84 (latitude/longitude), usado pelo PostGIS.
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public GeocodingService(@Value("${google.maps.api.key}") String apiKey) {
        // Constrói o cliente da API do Google no início da aplicação.
        this.context = new GeoApiContext.Builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * Converte um objeto de Endereço em um Ponto geográfico (Point) usando a API de Geocoding.
     * @param address O objeto de endereço da promoção.
     * @return Um Optional contendo o Point se o endereço for encontrado, ou um Optional vazio caso contrário.
     */
    public Optional<Point> geocodeAddress(Address address) {
        if (address == null || address.getAddress() == null || address.getNumber() == null) {
            return Optional.empty();
        }
        StringBuilder addressBuilder = new StringBuilder();
        addressBuilder.append(address.getAddress()).append(", ").append(address.getNumber());

        // Adiciona a cidade se ela foi preenchida
        if (address.getCity() != null && !address.getCity().isBlank()) {
            addressBuilder.append(", ").append(address.getCity());
        }

        // Adiciona o estado (UF) se ele foi preenchido
        if (address.getUF() != null && !address.getUF().isBlank()) {
            addressBuilder.append(" - ").append(address.getUF());
        }

        // Adiciona o CEP se ele foi preenchido
        if (address.getPostalCode() != null && !address.getPostalCode().isBlank()) {
            addressBuilder.append(", ").append(address.getPostalCode());
        }

        addressBuilder.append(", Brasil");

        String addressString = addressBuilder.toString();
        // Monta a string de endereço. Adicionar cidade e estado no futuro melhorará a precisão.

        try {
            GeocodingResult[] results = GeocodingApi.geocode(context, addressString).await();

            if (results != null && results.length > 0) {
                double lat = results[0].geometry.location.lat;
                double lng = results[0].geometry.location.lng;

                logger.info("Endereço '{}' geocodificado para Lat: {}, Lng: {}", addressString, lat, lng);

                // A ordem de coordenadas no JTS é (longitude, latitude)
                Point point = geometryFactory.createPoint(new Coordinate(lng, lat));
                return Optional.of(point);
            }
        } catch (Exception e) {
            logger.error("Erro ao tentar geocodificar o endereço: {}", addressString, e);
        }

        logger.warn("Nenhum resultado de geocodificação encontrado para o endereço: {}", addressString);
        return Optional.empty();
    }
}