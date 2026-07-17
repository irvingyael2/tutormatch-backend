package com.tutormatch.ms_evaluaciones.service;

import com.tutormatch.ms_evaluaciones.dto.EvaluacionRequestDto;
import com.tutormatch.ms_evaluaciones.dto.EvaluacionResponseDto;
import com.tutormatch.ms_evaluaciones.entity.Evaluacion;
import com.tutormatch.ms_evaluaciones.repository.EvaluacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EvaluacionService {

    private final EvaluacionRepository evaluacionRepository;

    public EvaluacionService(EvaluacionRepository evaluacionRepository) {
        this.evaluacionRepository = evaluacionRepository;
    }

    @Transactional
    public EvaluacionResponseDto crearEvaluacion(EvaluacionRequestDto dto, UUID alumnoId) {
        if (dto.getCalificacion() == null || dto.getCalificacion() < 1 || dto.getCalificacion() > 5) {
            throw new IllegalArgumentException("La calificación debe estar entre 1 y 5.");
        }

        Evaluacion evaluacion = new Evaluacion();
        evaluacion.setTutorId(dto.getTutorId());
        evaluacion.setAlumnoId(alumnoId);
        evaluacion.setSesionId(dto.getSesionId());
        evaluacion.setCalificacion(dto.getCalificacion());
        evaluacion.setComentario(dto.getComentario());

        Evaluacion guardada = evaluacionRepository.save(evaluacion);
        
        return new EvaluacionResponseDto(
                guardada.getId(),
                guardada.getTutorId(),
                guardada.getAlumnoId(),
                guardada.getSesionId(),
                guardada.getCalificacion(),
                guardada.getComentario(),
                guardada.getCreadoEn()
        );
    }

    @Transactional(readOnly = true)
    public Double obtenerPromedioTutor(UUID tutorId) {
        Double promedio = evaluacionRepository.obtenerPromedioPorTutorId(tutorId);
        return promedio != null ? Math.round(promedio * 10.0) / 10.0 : 0.0;
    }

    @Transactional(readOnly = true)
    public java.util.Map<UUID, Double> obtenerPromediosPorTutorIds(java.util.List<UUID> tutorIds) {
        if (tutorIds == null || tutorIds.isEmpty()) return java.util.Collections.emptyMap();
        
        java.util.List<Object[]> resultados = evaluacionRepository.obtenerPromediosPorTutorIds(tutorIds);
        java.util.Map<UUID, Double> map = new java.util.HashMap<>();
        for (Object[] obj : resultados) {
            UUID id = (UUID) obj[0];
            Double promedio = (Double) obj[1];
            map.put(id, promedio != null ? Math.round(promedio * 10.0) / 10.0 : 0.0);
        }
        return map;
    }

    @Transactional(readOnly = true)
    public java.util.List<UUID> obtenerSesionesEvaluadasPorAlumno(UUID alumnoId) {
        return evaluacionRepository.findSesionesEvaluadasPorAlumno(alumnoId);
    }
}
