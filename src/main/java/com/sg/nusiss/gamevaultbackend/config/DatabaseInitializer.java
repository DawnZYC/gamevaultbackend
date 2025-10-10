package com.sg.nusiss.gamevaultbackend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 数据库初始化器
 * 在应用启动时执行SQL脚本来创建论坛相关表
 */
@Component
public class DatabaseInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("开始初始化数据库表结构...");
        
        try {
            // 读取并执行完整的数据库脚本
            executeSqlScript("sql/complete_schema.sql");
            logger.info("数据库表结构初始化完成");
        } catch (Exception e) {
            logger.error("数据库初始化失败", e);
            // 不抛出异常，避免影响应用启动
        }
    }
    
    /**
     * 执行SQL脚本
     */
    private void executeSqlScript(String scriptPath) throws IOException {
        ClassPathResource resource = new ClassPathResource(scriptPath);
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            
            StringBuilder sql = new StringBuilder();
            String line;
            boolean inFunctionBody = false;  // 标记是否在函数体内
            
            while ((line = reader.readLine()) != null) {
                // 跳过注释和空行
                if (line.trim().startsWith("--") || line.trim().isEmpty()) {
                    continue;
                }
                
                sql.append(line).append("\n");
                
                // 检查是否进入或退出函数体（使用 $$ 分隔符）
                String trimmedLine = line.trim();
                if (trimmedLine.contains("$$")) {
                    // 统计当前行中 $$ 的出现次数
                    int count = 0;
                    int index = 0;
                    while ((index = trimmedLine.indexOf("$$", index)) != -1) {
                        count++;
                        index += 2;
                    }
                    // 如果出现奇数次 $$，则切换函数体状态
                    if (count % 2 == 1) {
                        inFunctionBody = !inFunctionBody;
                    }
                }
                
                // 如果遇到分号且不在函数体内，执行SQL语句
                if (trimmedLine.endsWith(";") && !inFunctionBody) {
                    String sqlStatement = sql.toString().trim();
                    if (!sqlStatement.isEmpty()) {
                        try {
                            jdbcTemplate.execute(sqlStatement);
                            logger.debug("执行SQL成功");
                        } catch (Exception e) {
                            // 只记录关键错误信息，不打印完整SQL
                            logger.warn("执行SQL失败: {}", e.getMessage());
                        }
                    }
                    sql.setLength(0);
                }
            }
        }
    }
}
