# User Authentication and Authorization App
[![Build Status][travis-image]][travis-url]
[![Sonar Status][sonar-image]][sonar-url]

[travis-image]: https://travis-ci.com/Dhomoni/uaa-app.svg?branch=master
[travis-url]: https://travis-ci.com/Dhomoni/uaa-app

[sonar-image]: https://sonarcloud.io/api/project_badges/measure?project=com.dhomoni.uaa%3Auaa&metric=alert_status
[sonar-url]: https://sonarcloud.io/dashboard?id=com.dhomoni.uaa%3Auaa

This application was generated using JHipster 5.6.1, you can find documentation and help at [https://www.jhipster.tech/documentation-archive/v5.6.1](https://www.jhipster.tech/documentation-archive/v5.6.1).

This is a "uaa" application intended to be part of a microservice architecture, please refer to the [Doing microservices with JHipster][] page of the documentation for more information.

This is also a JHipster User Account and Authentication (UAA) Server, refer to [Using UAA for Microservice Security][] for details on how to secure JHipster microservices with OAuth2.
This application is configured for Service Discovery and Configuration with . On launch, it will refuse to start if it is not able to connect to .

## Development

### How to add new roles in microservice architectures of Jhipster:
All Roles: ADMIN, PATIENT, DOCTOR, USER(common)

#### UAA server:
1. Add user to src/main/resources/config/liquibase/users.csv
5;doctor;$2a$10$VEjxo0jq2YG9Rbk2HmX9S.k1uZBGYUHdUcid3g/vfiEl7lwWgOH/K;Doctor;Doctor;doctor@localhost;;true;en;system;system

2. Add role to src/main/resources/config/liquibase/authorities.csv
ROLE_DOCTOR

3. Add entry to src/main/resources/config/liquibase/users_authorities.csv
5;ROLE_DOCTOR

5. Add role to src/main/java/com/dhomoni/gateway/security/AuthoritiesConstants.java
    public static final String DOCTOR = "ROLE_DOCTOR";

6. Login with username: doctor and password: user

#### Gateway server:
1. Add role to src/main/webapp/app/config/constants.ts
export const AUTHORITIES = {
  ADMIN: 'ROLE_ADMIN',
  USER: 'ROLE_USER' ,
  DOCTOR: 'ROLE_DOCTOR'
};

2. Add routing access in src/main/webapp/app/routes.tsx file

3. Add role to src/main/java/com/dhomoni/gateway/security/AuthoritiesConstants.java
    public static final String DOCTOR = "ROLE_DOCTOR";

### How to enable H2 console in dev profile:
    h2:
        console:
            enabled: true

### How to add spatial capability (postgresql + postgis) in the jhipster project:
```
1. add repository in pom.xml
    <repository>
        <id>boundless</id>
        <url>https://repo.boundlessgeo.com/main</url>
    </repository>

2. add properties in pom.xml
    <properties>
        <hibernate-spatial.version>5.2.17.Final</hibernate-spatial.version>
        <liquibase-spatial.version>1.2.1</liquibase-spatial.version>
        <geodb.version>0.9</geodb.version>
        <jackson-datatype-jts.version>2.4</jackson-datatype-jts.version>
    </properties>

3. add hibernate-spatial, liquibase-spatial, geodb and jackson-datatype-jts dependencies to pom.xml
    <dependency>
        <groupId>org.hibernate</groupId>
        <artifactId>hibernate-spatial</artifactId>
    </dependency>
    <dependency>
        <groupId>com.github.lonnyj</groupId>
        <artifactId>liquibase-spatial</artifactId>
        <version>${liquibase-spatial.version}</version>
    </dependency>
    <dependency>
        <groupId>org.opengeo</groupId>
        <artifactId>geodb</artifactId>
        <version>${geodb.version}</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>com.bedatadriven</groupId>
        <artifactId>jackson-datatype-jts</artifactId>
        <version>${jackson-datatype-jts.version}</version>
    </dependency>

4. add geodb dependency under dev profile
    <dependency>
        <groupId>org.opengeo</groupId>
        <artifactId>geodb</artifactId>
        <version>${geodb.version}</version>
    </dependency>

5. add dependencies under liquibase-maven-plugin
    <dependency>
        <groupId>org.hibernate</groupId>
        <artifactId>hibernate-spatial</artifactId>
        <version>${hibernate-spatial.version}</version>
    </dependency>
    <dependency>
        <groupId>com.github.lonnyj</groupId>
        <artifactId>liquibase-spatial</artifactId>
        <version>${liquibase-spatial.version}</version>
    </dependency>

6. use GeoDBDialect in liquibase-maven-plugin
    <referenceUrl>hibernate:spring:com.dhomoni.uaa.domain?dialect=org.hibernate.spatial.dialect.h2geodb.GeoDBDialect&amp;hibernate.physical_naming_strategy=org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy&amp;hibernate.implicit_naming_strategy=org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy
    </referenceUrl>

7. change dialect in application-dev.yml
    jpa:
        database-platform: org.hibernate.spatial.dialect.h2geodb.GeoDBDialect

8. change dialect in application-prod.yml
    jpa:
        database-platform: org.hibernate.spatial.dialect.postgis.PostgisDialect

9. add the foolowing segment in 00000000000000_initial_schema.xml at the top
    <changeSet id="spatial_db" author="pervez" dbms="h2">
    	<sql dbms="h2">CREATE ALIAS InitGeoDB for "geodb.GeoDB.InitGeoDB"</sql>
    	<sql dbms="h2">CALL InitGeoDB()</sql>
    	<rollback>
        	<sql dbms="h2">DROP ALIAS InitGeoDB</sql>
    	</rollback>
    </changeSet>

10. make the table name in capital letters as the following example in liquibase chengelog
    <createTable tableName="DOCTOR">
        <column name="GEOM" type="GEOMETRY(Point, 4326)"/>
    </createTable>  

11. add the following segment in the entity
    import com.vividsolutions.jts.geom.Point;

    @Column(name = "GEOM", columnDefinition = "GEOMETRY(Point, 4326)")
    private Point location;

14. add following snippet in JacksonConfiguration

	@Bean
	public JtsModule jtsModule() {
		GeometryFactory gf = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);
		return new JtsModule(gf);
	}

15. update following method in TestUtil
    public static byte[] convertObjectToJsonBytes(Object object)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        JavaTimeModule module = new JavaTimeModule();
        mapper.registerModule(module);
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);
        JtsModule jtsModule = new JtsModule(gf);
        mapper.registerModule(jtsModule);
        Jdk8Module jdk8Module = new Jdk8Module();
        mapper.registerModule(jdk8Module);
        return mapper.writeValueAsBytes(object);
    }
```

### Notes on development environment:
```
01. Install Java

02. Install STS

03. Install npm

04. Install docker

05. Install docker-compose :
		$ sudo apt install docker-compose

06. Install jhipster :
		$ npm install -g generator-jhipster

07. Build postgresql-postgis docker image :
		$ cd uaa-app/src/main/docker/postgis/
		$ docker build -t postgresql-postgis:10.6 .   

08. Pull and Deploy sonar :
		$ docker-compose -f src/main/docker/sonar.yml up -d	

09. Build and deploy uaa-app :
		$ cd uaa-app/
		$ ./mvnw clean package -Pprod sonar:sonar jib:dockerBuild
		$ docker-compose -f src/main/docker/app.yml up

11. Build and deploy gateway-app :
		$ cd gateway-app/
		$ npm install
		$ ./mvnw clean package -Pprod sonar:sonar jib:dockerBuild
		$ docker-compose -f src/main/docker/app.yml up

12. Build and deploy diagnosys-app :
		$ cd diagnosys-app/
		$ ./mvnw clean package -Pprod sonar:sonar jib:dockerBuild
		$ docker-compose -f src/main/docker/app.yml up

13. Build and deploy search-app :
		$ cd search-app/
		$ ./mvnw clean package -Pprod sonar:sonar jib:dockerBuild
		$ docker-compose -f src/main/docker/app.yml up

14. Additional development notes :
    To generate brand new microeservice app from jdl(example):
        jhipster import-jdl search-app.jdl
       
    To remove files with wildcard search:
        find . -name .Weekly* -exec rm -rf {} +

    To build docker image of postgres with postgis :
        docker build -t postgresql-postgis:10.6 .
	
    In development you may need this for quick web debug :
        $ npm start

    To generate diff script of db and entity :
        $ ./mvnw compile liquibase:diff
    
    To clear liquibase changelog checksum :
        $ docker rm $(docker ps -a -q)
```
```
Helpful commands for docker:
docker container ps -a
docker container stats
docker container stop
```
```
docker rm $(docker ps -a -q)
docker rmi $(docker images -q)
docker kill $(docker ps -q)
```
```
db clean up:
delete from jhi_user_authority where user_id = (select id from jhi_user where login = 'pervez');
delete from jhi_user where login = 'pervez';
delete from degree;
```




## Building for production

To optimize the uaa application for production, run:


To ensure everything worked, run:



Refer to [Using JHipster in production][] for more details.

## Testing

To launch your application's tests, run:

    ./gradlew test
### Other tests

Performance tests are run by [Gatling][] and written in Scala. They're located in [src/test/gatling](src/test/gatling).

To use those tests, you must install Gatling from [https://gatling.io/](https://gatling.io/).

For more information, refer to the [Running tests page][].

### Code quality

Sonar is used to analyse code quality. You can start a local Sonar server (accessible on http://localhost:9001) with:

```
docker-compose -f src/main/docker/sonar.yml up -d
```

Then, run a Sonar analysis:

```
./gradlew -Pprod clean test sonarqube
```

For more information, refer to the [Code quality page][].

## Using Docker to simplify development (optional)

You can use Docker to improve your JHipster development experience. A number of docker-compose configuration are available in the [src/main/docker](src/main/docker) folder to launch required third party services.

For example, to start a  database in a docker container, run:

    docker-compose -f src/main/docker/.yml up -d

To stop it and remove the container, run:

    docker-compose -f src/main/docker/.yml down

You can also fully dockerize your application and all the services that it depends on.
To achieve this, first build a docker image of your app by running:

    

Then run:

    docker-compose -f src/main/docker/app.yml up -d

For more information refer to [Using Docker and Docker-Compose][], this page also contains information on the docker-compose sub-generator (`jhipster docker-compose`), which is able to generate docker configurations for one or several JHipster applications.

## Continuous Integration (optional)

To configure CI for your project, run the ci-cd sub-generator (`jhipster ci-cd`), this will let you generate configuration files for a number of Continuous Integration systems. Consult the [Setting up Continuous Integration][] page for more information.

[JHipster Homepage and latest documentation]: https://www.jhipster.tech
[JHipster 5.6.1 archive]: https://www.jhipster.tech/documentation-archive/v5.6.1
[Doing microservices with JHipster]: https://www.jhipster.tech/documentation-archive/v5.6.1/microservices-architecture/
[Using UAA for Microservice Security]: https://www.jhipster.tech/documentation-archive/v5.6.1/using-uaa/[Using JHipster in development]: https://www.jhipster.tech/documentation-archive/v5.6.1/development/
[Using Docker and Docker-Compose]: https://www.jhipster.tech/documentation-archive/v5.6.1/docker-compose
[Using JHipster in production]: https://www.jhipster.tech/documentation-archive/v5.6.1/production/
[Running tests page]: https://www.jhipster.tech/documentation-archive/v5.6.1/running-tests/
[Code quality page]: https://www.jhipster.tech/documentation-archive/v5.6.1/code-quality/
[Setting up Continuous Integration]: https://www.jhipster.tech/documentation-archive/v5.6.1/setting-up-ci/

[Gatling]: http://gatling.io/
