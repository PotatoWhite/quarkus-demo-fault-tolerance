package me.potato.demo.model;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RequiredArgsConstructor
@Path("/coffee")
public class CoffeeResource {
  private final CoffeeRepositoryService coffeeRepository;

  private AtomicLong counter = new AtomicLong(0);

  // Retry
  @GET
  @Retry(maxRetries = 4)
  public List<Coffee> coffees() throws Exception {
    final Long invocationNumber = counter.getAndIncrement();
    maybeFail(String.format("CoffeeResource#Coffees() invocation #%d failed", invocationNumber));
    log.info("CoffeeResource#Coffees() invocation #{} returning successfully", invocationNumber);
    return coffeeRepository.getAllCoffees();
  }

  private void maybeFail(String failureLogMessage) throws Exception {
    if(new Random().nextBoolean()) {
      log.error(failureLogMessage);
      throw new InternalServerErrorException("Resource failure.");
    }
  }

  // Timeout & fall back
  @GET
  @Path("/{id}/recommendations")
  @Timeout(250)
  @Fallback(fallbackMethod = "fallbackRecommendations")
  public List<Coffee> recommendations(@PathParam("id") int id) {
    long       started          = System.currentTimeMillis();
    final long invocationNumber = counter.getAndIncrement();

    try {
      randomDelay();
      log.info("CoffeeResource#Recomendations() invocation #{} returning successfully", invocationNumber);
      return coffeeRepository.getRecommendations(id);
    } catch(InterruptedException e) {
      log.info("CoffeeResource#Recomendations() invocation #{} timed out after {} ms", invocationNumber, System.currentTimeMillis()-started);
    }

    return null;

  }

  public List<Coffee> fallbackRecommendations(int id) {
    log.info("Falling back to CoffeeResource#fallbackRecommendations()");
    return Collections.singletonList(coffeeRepository.getCoffeeById(1));
  }

  private void randomDelay() throws InterruptedException {
    Thread.sleep(new Random().nextInt(500));
  }


  // Circuit Break
  @Path("/{id}/availability")
  @GET
  public Response availability(@PathParam int id) {
    final Long invocationNumber = counter.getAndIncrement();
    Coffee     coffee           = coffeeRepository.getCoffeeById(id);

    if(coffee == null)
      return Response.status(Response.Status.NOT_FOUND)
                     .build();

    try {
      Integer availability = coffeeRepository.getAvailability(coffee);
      log.info("CoffeeResource@availability() invocation #{} returning success", invocationNumber);
      return Response.ok(availability)
                     .build();
    } catch(RuntimeException e) {
      log.error("CoffeeResource@availability() invocation #{} failed: %s", invocationNumber, e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                     .entity(e.getMessage())
                     .type(MediaType.TEXT_PLAIN_TYPE)
                     .build();
    }

  }


}
