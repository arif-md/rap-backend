package x.y.z.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@MapperScan("x.y.z.backend.repository.mapper")
public class BackendApplication {

	public static void main(String[] args) {
		System.out.println("Starting Raptor Backend Application...");
		SpringApplication.run(BackendApplication.class, args);
	}

}
