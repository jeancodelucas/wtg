package com.projects.wtg.controller;

import com.projects.wtg.dto.AddressDto;
import com.projects.wtg.model.Address;
import com.projects.wtg.service.GeocodingService;
import jakarta.validation.Valid;
import org.locationtech.jts.geom.Point;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/geocoding")
public class GeocodingController {

    private final GeocodingService geocodingService;

    public GeocodingController(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    @PostMapping("/from-address")
    public ResponseEntity<?> getCoordinatesFromAddress(@Valid @RequestBody AddressDto addressDto) {
        // Converte o DTO para a entidade Address para usar no serviço
        Address address = new Address();
        address.setAddress(addressDto.getAddress());
        address.setNumber(addressDto.getNumber());
        address.setPostalCode(addressDto.getPostalCode());

        Optional<Point> pointOptional = geocodingService.geocodeAddress(address);

        if (pointOptional.isPresent()) {
            Point point = pointOptional.get();
            // Retorna um JSON simples com latitude e longitude, que é o que o frontend precisa
            Map<String, Double> coordinates = Map.of(
                    "latitude", point.getY(),
                    "longitude", point.getX()
            );
            return ResponseEntity.ok(coordinates);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Não foi possível encontrar coordenadas para o endereço fornecido."));
        }
    }
}