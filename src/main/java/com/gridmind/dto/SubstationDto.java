package com.gridmind.dto;

import com.gridmind.domain.GridStatus;
import com.gridmind.domain.SubstationType;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubstationDto {
    private String id;
    private String code;
    private String name;
    private String location;
    private SubstationType type;
    private GridStatus status;
    private ZonedDateTime createdAt;
    private List<TransformerDto> transformers;
}
