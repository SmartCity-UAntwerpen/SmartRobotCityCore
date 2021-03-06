package be.uantwerpen.sc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
@ComponentScan("be.uantwerpen.*") //Look for components locally and in the robot common
@EntityScan("be.uantwerpen.*") //Look for entities locally and in the robot common
@SpringBootApplication//(exclude = {EmbeddedServletContainerAutoConfiguration.class})
public class RobotBackendApplication extends SpringBootServletInitializer
{
	public static void main(String[] args)
	{
		SpringApplication.run(RobotBackendApplication.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder applicationBuilder)
	{
		return applicationBuilder.sources(RobotBackendApplication.class);
	}
}
