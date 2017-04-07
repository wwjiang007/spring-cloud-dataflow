/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.server.controller;

import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.server.repository.DeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.DeploymentKey;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.support.SearchPageable;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for operations on {@link TaskDefinition}.  This includes CRUD operations.
 *
 * @author Michael Minella
 * @author Marius Bogoevici
 * @author Glenn Renfro
 * @author Mark Fisher
 */
@RestController
@RequestMapping("/tasks/definitions")
@ExposesResourceFor(TaskDefinitionResource.class)
public class TaskDefinitionController {

	private final Assembler taskAssembler = new Assembler();

	private TaskDefinitionRepository repository;

	/**
	 * The repository this controller will use for deployment IDs.
	 */
	private final DeploymentIdRepository deploymentIdRepository;

	private TaskLauncher taskLauncher;

	private TaskService taskService;

	/**
	 * The app registry this controller will use to lookup apps.
	 */
	private final AppRegistry appRegistry;

	/**
	 * Creates a {@code TaskDefinitionController} that delegates
	 * <ul>
	 *     <li>CRUD operations to the provided {@link TaskDefinitionRepository}</li>
	 *     <li>task status checks to the provided {@link TaskLauncher}</li>
	 * </ul>
	 *
	 * @param repository the repository this controller will use for task CRUD operations.
	 * @param deploymentIdRepository the repository this controller will use for deployment IDs
	 * @param taskLauncher the TaskLauncher this controller will use to check task status.
	 * @param appRegistry the app registry to look up registered apps.
	 * @param taskService handles specialized behavior needed for tasks.
	 *
	 */
	public TaskDefinitionController(TaskDefinitionRepository repository, DeploymentIdRepository deploymentIdRepository,
			TaskLauncher taskLauncher, AppRegistry appRegistry, TaskService taskService) {
		Assert.notNull(repository, "repository must not be null");
		Assert.notNull(deploymentIdRepository, "deploymentIdRepository must not be null");
		Assert.notNull(taskLauncher, "taskLauncher must not be null");
		Assert.notNull(appRegistry, "appRegistry must not be null");
		Assert.notNull(taskService, "taskService must not be null");
		this.repository = repository;
		this.deploymentIdRepository = deploymentIdRepository;
		this.taskLauncher = taskLauncher;
		this.appRegistry = appRegistry;
		this.taskService = taskService;
	}

	/**
	 * Register a task definition for future execution.
	 *
	 * @param name name the name of the task
	 * @param dsl DSL definition for the task
	 */
	@RequestMapping(value = "", method = RequestMethod.POST)
	public TaskDefinitionResource save(@RequestParam("name") String name,
			@RequestParam("definition") String dsl) {
		TaskDefinition taskDefinition = new TaskDefinition(name, dsl);
		taskService.saveTaskDefinition(name, dsl);
		return taskAssembler.toResource(taskDefinition);
	}

	/**
	 * Delete the task from the repository so that it can no longer be executed.
	 *
	 * @param name name of the task to be deleted
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	public void destroyTask(@PathVariable("name") String name) {
		taskService.deleteTaskDefinition(name);
	}

	/**
	 * Return a page-able list of {@link TaskDefinitionResource} defined tasks.
	 *
	 * @param pageable  page-able collection of {@code TaskDefinitionResource}.
	 * @param assembler assembler for the {@link TaskDefinition}
	 * @param search optional search parameter
	 * @return a list of task definitions
	 */
	@RequestMapping(value="", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<TaskDefinitionResource> list(Pageable pageable, @RequestParam(required=false) String search,
			PagedResourcesAssembler<TaskDefinition> assembler) {

		if (search != null) {
			final SearchPageable searchPageable = new SearchPageable(pageable, search);
			searchPageable.addColumns("DEFINITION_NAME", "DEFINITION");
			return assembler.toResource(repository.search(searchPageable), taskAssembler);
		}
		else {
			return assembler.toResource(repository.findAll(pageable), taskAssembler);
		}
	}


	/**
	 * Return a given task definition resource.
	 * @param name the name of an existing task definition (required)
	 */
	@RequestMapping(value = "/{name}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public TaskDefinitionResource display(@PathVariable("name") String name) {
		TaskDefinition definition = repository.findOne(name);
		if (definition == null) {
			throw new NoSuchTaskDefinitionException(name);
		}
		return taskAssembler.toResource(definition);
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation
	 * that converts {@link TaskDefinition}s to {@link TaskDefinitionResource}s.
	 */
	class Assembler extends ResourceAssemblerSupport<TaskDefinition, TaskDefinitionResource> {

		public Assembler() {
			super(TaskDefinitionController.class, TaskDefinitionResource.class);
		}

		@Override
		public TaskDefinitionResource toResource(TaskDefinition taskDefinition) {
			return createResourceWithId(taskDefinition.getName(), taskDefinition);
		}

		@Override
		public TaskDefinitionResource instantiateResource(TaskDefinition taskDefinition) {
			String key = DeploymentKey.forTaskDefinition(taskDefinition);
			String id = deploymentIdRepository.findOne(key);
			TaskStatus status = null;
			if (id != null) {
				status = taskLauncher.status(id);
			}
			String state = (status != null) ? status.getState().name() : "unknown";
			TaskDefinitionResource taskDefinitionResource = new TaskDefinitionResource(
					taskDefinition.getName(),
					taskDefinition.getDslText());
			taskDefinitionResource.setStatus(state);
			return taskDefinitionResource;
		}
	}
}
