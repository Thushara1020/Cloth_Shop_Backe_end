package edu.icet.ecom.controller;

import edu.icet.ecom.model.dto.GrnDto;
import edu.icet.ecom.service.GrnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grn")
@RequiredArgsConstructor
@CrossOrigin()
public class GrnController {

    private final GrnService grnService;

    @PostMapping("/process")
    public ResponseEntity<String> processGrn(@RequestBody GrnDto grnDto) {
        grnService.processGrn(grnDto);
        return ResponseEntity.ok("GRN Processed successfully. Stock and Batches updated!");
    }
}