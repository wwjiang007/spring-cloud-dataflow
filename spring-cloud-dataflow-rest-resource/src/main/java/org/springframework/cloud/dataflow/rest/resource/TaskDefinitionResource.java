/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.cloud.dataflow.rest.resource;

import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;

/**
 * A HATEOAS representation of a {@link TaskDefinition}.
 *
 * @author Michael Minella
 * @author Glenn Renfro
 */
public class TaskDefinitionResource extends ResourceSupport {

	private String name;

	private String dslText;

	/**
	 * Stream status (i.e. running, complete, etc).
	 */
	private String status;

	/**
	 * Default constructor to be used by Jackson.
	 */
	@SuppressWarnings("unused")
	protected TaskDefinitionResource() {
	}

	public TaskDefinitionResource(String name, String dslText) {
		this.name = name;
		this.dslText = dslText;
	}

	public String getName() {
		return name;
	}

	public String getDslText() {
		return dslText;
	}

	/**
	 * Return the status of this task (i.e. running, complete, etc)
	 *
	 * @return task status
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Set the status of this task (i.e. running, complete, etc)
	 *
	 * @param status task status
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	public static class Page extends PagedResources<TaskDefinitionResource>{}
}
