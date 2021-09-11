package com.imooc.activiti.helloWorld;

import com.google.common.collect.Maps;
import org.activiti.engine.*;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.impl.form.DateFormType;
import org.activiti.engine.impl.form.StringFormType;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/**
 * 启动类
 */
public class DemoMain {
    private static final Logger logger = LoggerFactory.getLogger(DemoMain.class);

    public static void main(String[] args) throws ParseException {
        logger.info("启动我们的程序");
        // 1.创建流程引擎
        ProcessEngine processEngine = getProcessEngine();
        // 2.部署流程定义文件
        ProcessDefinition processDefinition = getProcessDefinition(processEngine);
        // 3.启动运行流程
        ProcessInstance processInstance = getProcessInstance(processEngine, processDefinition);

        // 4.处理流程任务
        handleTask(processEngine, processInstance);
        logger.info("结束我们的程序");
    }

    /**
     * 4.处理流程任务
     * @param processEngine
     * @param processInstance
     * @throws ParseException
     */
    private static void handleTask(ProcessEngine processEngine, ProcessInstance processInstance) throws ParseException {
        Scanner scanner = new Scanner(System.in);
        while (processInstance != null && !processInstance.isEnded()) {
            TaskService taskService = processEngine.getTaskService();
            List<Task> list = taskService.createTaskQuery().list();
            logger.info("待处理任务数量：{}", list.size());
            for (Task task : list) {
                logger.info("待处理任务：{}",task.getName());
                FormService formService = processEngine.getFormService();
                TaskFormData taskFormData = formService.getTaskFormData(task.getId());
                List<FormProperty> formProperties = taskFormData.getFormProperties();
                HashMap<String, Object> variables = Maps.newHashMap();
                for (FormProperty formProperty : formProperties) {
                    String line = null;
                    if (StringFormType.class.isInstance(formProperty.getType())) {
                        logger.info("请输入{}？", formProperty.getName());
                        line = scanner.nextLine();
                        variables.put(formProperty.getId(), line);
                    } else if (DateFormType.class.isInstance(formProperty.getType())) {
                        logger.info("请输入：{} 格式(yyyy-MM-dd)", formProperty.getName());
                        line = scanner.nextLine();
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        Date date = simpleDateFormat.parse(line);
                        variables.put(formProperty.getId(), date);
                    } else {
                        logger.info("类型暂不支持", formProperty.getType());
                    }
                    logger.info("你输入的内容是：{}", line);
                }
                // 提交工作
                taskService.complete(task.getId(), variables);
                // 执行完一次，重新获取流程实例进行状态判断
                 processInstance = processEngine.getRuntimeService()
                        .createProcessInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .singleResult();
            }
        }
    }

    /**
     * 3.启动运行流程
     * @param processEngine
     * @param processDefinition
     * @return
     */
    private static ProcessInstance getProcessInstance(ProcessEngine processEngine, ProcessDefinition processDefinition) {
        RuntimeService runtimeService = processEngine.getRuntimeService();
        ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefinition.getId());
        logger.info("启动流程：{}", processInstance.getProcessDefinitionKey());
        return processInstance;
    }

    /**
     * 2.部署流程定义文件
     * @param processEngine
     * @return
     */
    private static ProcessDefinition getProcessDefinition(ProcessEngine processEngine) {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
        deploymentBuilder.addClasspathResource("second_approve.bpmn20.xml");
        Deployment deployment = deploymentBuilder.deploy();
        String deploymentId = deployment.getId();
        // 获取流程定义对象
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deploymentId)
                .singleResult();
        logger.info("流程定义文件名称：{},流程id：{}", processDefinition.getName(), processDefinition.getId());
        return processDefinition;
    }

    /**
     *   1.创建流程引擎
     * @return
     */
    private static ProcessEngine getProcessEngine() {
        // 创建基于内存数据库的流程引擎
        ProcessEngineConfiguration cfg = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
        ProcessEngine processEngine = cfg.buildProcessEngine();
        String name = processEngine.getName();
        String version = ProcessEngine.VERSION;

        logger.info("流程引擎名称{},版本{}", name, version);
        return processEngine;
    }

}
