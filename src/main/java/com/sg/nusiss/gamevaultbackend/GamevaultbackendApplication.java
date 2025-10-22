package com.sg.nusiss.gamevaultbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
        scanBasePackages = {
                "com.sg.nusiss.gamevaultbackend"
        }
)
@EntityScan(basePackages = {
        "com.sg.nusiss.gamevaultbackend.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.sg.nusiss.gamevaultbackend.repository"
})
@MapperScan(basePackages = {
        "com.sg.nusiss.gamevaultbackend.mapper"
})
public class GamevaultbackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GamevaultbackendApplication.class, args);
    }

}
