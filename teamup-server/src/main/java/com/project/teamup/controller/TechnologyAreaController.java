package com.project.teamup.controller;


import com.project.teamup.controller.exception.ObjectNotValidException;
import com.project.teamup.dto.TechnologyAreaDTO;
import com.project.teamup.mapper.TechnologyAreaMapper;
import com.project.teamup.service.TechnologyAreaService;
import org.hibernate.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/technology-area")
public class TechnologyAreaController {
    @Autowired
    private TechnologyAreaService technologyAreaService;
    @Autowired
    private TechnologyAreaMapper technologyAreaMapper;

    @PostMapping(value = "/save")
    public TechnologyAreaDTO save(@RequestBody TechnologyAreaDTO technologyArea) throws ObjectNotValidException {
        return technologyAreaMapper.toDto(technologyAreaService.save(technologyAreaMapper.toEntity(technologyArea)));
    }

    @GetMapping
    public List<TechnologyAreaDTO> getAll() throws ObjectNotFoundException {
        return technologyAreaMapper.toDtoList(technologyAreaService.getAll());
    }

    @GetMapping("/{id}")
    public TechnologyAreaDTO findById(@PathVariable("id") Long id) throws ObjectNotFoundException {
        return technologyAreaService.findById(id)
                .map(technologyArea -> technologyAreaMapper.toDto(technologyArea))
                .orElse(null);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(value = HttpStatus.OK)
    public void delete(@PathVariable("id") Long id) throws ObjectNotFoundException {
        technologyAreaService.delete(id);
    }
}
