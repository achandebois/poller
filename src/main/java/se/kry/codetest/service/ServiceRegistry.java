package se.kry.codetest.service;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import se.kry.codetest.model.Service;
import se.kry.codetest.model.ServiceStatus;
import se.kry.codetest.repository.ServiceRegistryRepository;

import java.util.List;
import java.util.regex.Pattern;

public class ServiceRegistry {

    private final Pattern pattern = Pattern.compile("http(s?):\\/\\/(www\\.)?[a-zA-Z0-9\\-\\.\\/]*");
    private final ServiceRegistryRepository repository;

    public ServiceRegistry(Vertx vertx) {
        this.repository = new ServiceRegistryRepository(vertx);
    }

    public Future<Boolean> deleteAll() {
        return repository.truncate();
    }

    public Future<List<Service>> getAll() {
        return repository.findAll();
    }

    public Future<String> create(String url, String name) {
        if (url != null && pattern.matcher(url).matches()) {
            return repository.save(url, name, ServiceStatus.UNKNOWN);
        } else {
            return Future.failedFuture(new IllegalArgumentException("Invalid url format"));
        }
    }

    public Future<Boolean> delete(String id) {
        if (id == null) {
            return Future.failedFuture(new IllegalArgumentException("Id to delete service must not be null"));
        }
        return repository.delete(id);
    }

    public Future<Boolean> updateStatus(String id, ServiceStatus status) {
        if (id == null) {
            return Future.failedFuture(new IllegalArgumentException("Id to update service must not be null"));
        }
        if (status == null) {
            return Future.failedFuture(new IllegalArgumentException("Status to update service must not be null"));
        }

        return repository.updateStatus(id, status);
    }

    public Future<Boolean> createDb() {
        return repository.createDb();
    }

    public Future<Boolean> update(Service service) {
        if (service.getUrl() != null && pattern.matcher(service.getUrl()).matches()) {
            return repository.update(service);
        } else {
            return Future.failedFuture(new IllegalArgumentException("Invalid url format"));
        }
    }
}
