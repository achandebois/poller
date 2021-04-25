package se.kry.codetest.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Service {
    private String id;
    private String name;
    private String url;
    @With
    private ServiceStatus serviceStatus;
    private LocalDateTime creationDate;
}
