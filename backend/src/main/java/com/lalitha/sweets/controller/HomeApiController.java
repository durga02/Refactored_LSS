
package com.lalitha.sweets.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import com.lalitha.sweets.dto.ProductDto;
import com.lalitha.sweets.service.ProductService;
import com.lalitha.sweets.repository.FestivalBannerRepository;
import com.lalitha.sweets.model.FestivalBanner;

@RestController
@RequestMapping("/api/home")
public class HomeApiController {

    @Autowired
    private ProductService productService;

    @Autowired
    private FestivalBannerRepository festivalBannerRepository;

    @GetMapping(value = "/featured", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProductDto> featured() {
        return productService.getFeatured().stream()
                .map(ProductDto::new).toList();
    }

    @GetMapping(value = "/sweet", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProductDto> sweet() {
        return productService.getByCategory("Sweet").stream()
                .map(ProductDto::new).toList();
    }

    @GetMapping(value = "/hot", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProductDto> hot() {
        return productService.getByCategory("Hot").stream()
                .map(ProductDto::new).toList();
    }

    @GetMapping(value = "/festival-banner",
                produces = MediaType.APPLICATION_JSON_VALUE)
    public FestivalBanner festivalBanner() {
        return festivalBannerRepository.findById(1L)
                .orElseGet(() -> festivalBannerRepository.save(
                        new FestivalBanner()));
    }
}
