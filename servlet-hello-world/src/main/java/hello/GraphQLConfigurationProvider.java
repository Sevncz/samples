package hello;

import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

import graphql.execution.AsyncExecutionStrategy;
import graphql.execution.SubscriptionExecutionStrategy;
import graphql.kickstart.execution.GraphQLQueryInvoker;
import graphql.kickstart.execution.config.DefaultExecutionStrategyProvider;
import graphql.kickstart.servlet.GraphQLConfiguration;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLSchema;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Duration;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

class GraphQLConfigurationProvider {

  private static final Logger log = LoggerFactory.getLogger(GraphQLConfigurationProvider.class);

  private static GraphQLConfigurationProvider instance;

  private GraphQLConfiguration configuration;

  Flux<String> flux;

  private GraphQLConfigurationProvider() {
    flux = Flux.just("pong").repeat(10).delayElements(Duration.ofSeconds(1)).share().publish().autoConnect();

    configuration = GraphQLConfiguration
        .with(createSchema())
        .with(GraphQLQueryInvoker.newBuilder()
            .withExecutionStrategyProvider(new DefaultExecutionStrategyProvider(
                new AsyncExecutionStrategy(),
                null,
                new SubscriptionExecutionStrategy()))
            .build())
        .build();
  }

  static GraphQLConfigurationProvider getInstance() {
    if (instance == null) {
      instance = new GraphQLConfigurationProvider();
    }
    return instance;
  }

  GraphQLConfiguration getConfiguration() {
    return configuration;
  }

  private GraphQLSchema createSchema() {
    TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(loadSchemaFile());

    RuntimeWiring runtimeWiring = newRuntimeWiring()
        .type(newTypeWiring("Query").dataFetcher("hello", new StaticDataFetcher("world")))
        .type(newTypeWiring("Subscription").dataFetcher("ping", pingFetcher()))
        .build();

    SchemaGenerator schemaGenerator = new SchemaGenerator();
    return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
  }

  private DataFetcher<Publisher<String>> pingFetcher() {
    return environment -> {
      log.info("Subscribe to ping");
      System.out.println("Subscribe");
      return Flux.from(flux);
    };
  }

  private Reader loadSchemaFile() {
    InputStream stream = getClass().getClassLoader().getResourceAsStream("schema.graphqls");
    return new InputStreamReader(stream);
  }

}
