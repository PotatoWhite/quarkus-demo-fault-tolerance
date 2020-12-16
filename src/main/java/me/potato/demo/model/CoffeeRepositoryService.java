package me.potato.demo.model;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import javax.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@ApplicationScoped
public class CoffeeRepositoryService {
  private Map<Integer, Coffee> coffeeList = new ConcurrentHashMap<>();
  // circuit breakers
  private AtomicLong           counter    = new AtomicLong(0);

  public CoffeeRepositoryService() {
    coffeeList.put(1, new Coffee(1, "Espresso", "Colombia", 23));
    coffeeList.put(2, new Coffee(2, "Whole Bean", "Bolivia", 18));
    coffeeList.put(3, new Coffee(3, "Dak Lak Filter", "Vietnam", 25));
  }

  public List<Coffee> getAllCoffees() {
    return new ArrayList<>(coffeeList.values());
  }

  public Coffee getCoffeeById(Integer id) {
    return coffeeList.get(id);
  }

  public List<Coffee> getRecommendations(Integer id) {
    if(id == null)
      return Collections.emptyList();

    return coffeeList.values()
                     .stream()
                     .filter(coffee -> !id.equals(coffee.getId()))
                     .limit(2)
                     .collect(Collectors.toList());
  }

  @CircuitBreaker(requestVolumeThreshold = 4)
  public Integer getAvailability(Coffee coffee) {
    maybeFail();
    return new Random().nextInt(30);
  }

  private void maybeFail() {
    final Long invocationNumber = counter.getAndIncrement();
    if(invocationNumber%4 > 1) {
      throw new RuntimeException("Service failed");
    }
  }
}
