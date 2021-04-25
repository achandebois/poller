package se.kry.codetest.mapper;

import io.vertx.core.json.JsonObject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import se.kry.codetest.model.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceMapper {
    public static final String SERVICE_ID = "id";
    public static final String SERVICE_URL = "url";
    public static final String SERVICE_NAME = "name";
    public static final String SERVICE_STATUS = "status";
    public static final String SERVICE_CREATION_DATE = "creation_date";

    public static List<JsonObject> toJsonObjects(List<Service> services) {
        return services.stream()
                .map(svc ->
                        new JsonObject()
                                .put(SERVICE_ID, svc.getId())
                                .put(SERVICE_URL, svc.getUrl())
                                .put(SERVICE_NAME, svc.getName())
                                .put(SERVICE_STATUS, svc.getServiceStatus().toString())
                                .put(SERVICE_CREATION_DATE, svc.getCreationDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                )
                .collect(Collectors.toList());
    }

    public static Service toService(String id, JsonObject jsonObject) {
        return Service.builder()
                .id(id)
                .name(jsonObject.getString(SERVICE_NAME, ""))
                .url(jsonObject.getString(SERVICE_URL))
                .build();
    }
}
