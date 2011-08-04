/**
 *  Copyright 2011 Wordnik, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wordnik.swagger.codegen.resource;

import com.wordnik.swagger.codegen.MethodArgument;
import com.wordnik.swagger.codegen.ResourceMethod;
import com.wordnik.swagger.codegen.config.DataTypeMappingProvider;
import com.wordnik.swagger.codegen.config.NamingPolicyProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * User: ramesh
 * Date: 3/31/11
 * Time: 7:54 AM
 */
public class EndpointOperation {

	public static String PARAM_TYPE_QUERY = "query";
	public static String PARAM_TYPE_PATH = "path";
	public static String PARAM_TYPE_BODY = "body";
	public static String PARAM_TYPE_HEADER = "header";
	private static String AUTH_TOKEN_PARAM_NAME = "auth_token";
	private static String API_KEY_PARAM_NAME = "api_key";	
	private static String FORMAT_PARAM_NAME = "format";	
	
	private static String AUTH_TOKEN_ARGUMENT_NAME = "authToken";	
	
    private String httpMethod;

    private String summary = "";

    private String notes = "";

    private boolean open;

    @Deprecated
    private List<Response> response;

    private String responseClass;

    private List<ModelField> parameters;
    
    private boolean deprecated;
    
    private ResourceMethod method;

    private List<String> tags;

    @Deprecated
    private String suggestedName;

    private String nickname;
    
	public String getHttpMethod() {
		return httpMethod;
	}

	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public boolean isOpen() {
		return open;
	}

	public void setOpen(boolean open) {
		this.open = open;
	}

	public List<Response> getResponse() {
		return response;
	}

	public void setResponse(List<Response> response) {
		this.response = response;
	}

    public String getResponseClass() {
        return responseClass;
    }

    public void setResponseClass(String responseClass) {
        this.responseClass = responseClass;
        this.setResponse(new ArrayList<Response>());
        Response response = new Response();
        response.setValueType(this.responseClass);
        this.getResponse().add(response);
    }

	public List<ModelField> getParameters() {
		return parameters;
	}

	public void setParameters(List<ModelField> parameters) {
		this.parameters = parameters;
	}
	
	public boolean isDeprecated() {
		return deprecated;
	}

	public void setDeprecated(boolean deprecated) {
		this.deprecated = deprecated;
	}
	
	public String getSuggestedName() {
		return suggestedName;
	}

    public void setSuggestedName(String suggestedName) {
        this.suggestedName = suggestedName;
    }

    public void setNickname(String nickname) {
		this.nickname = nickname;
        this.suggestedName = nickname;
	}

	public String getNickname() {
		return nickname;
	}

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }


	public ResourceMethod generateMethod(Endpoint endPoint, Resource resource, DataTypeMappingProvider dataTypeMapper, NamingPolicyProvider nameGenerator) {
		if(method == null){
			method = new ResourceMethod();
			//add method description
			method.setDescription(this.getSummary() + "\n " + getNotes());
			
			//add method name
			//get resource path for making web service call
			/**
			 * Logic for method names
			 * 1. remove all path parameters
			 * 2. Remove format path parameter
			 * 3. For POST add save 
			 * 4. For PUT add update
			 * 5. For DELETE add delete
			 * 6. For GET add get
			 * 7. Concatenate rest of the path with init caps
			 * 8. 
			 */

			String inputobjectName = nameGenerator.getInputObjectName(resource.generateClassName(nameGenerator), endPoint.getPath());
			
			String[] pathElements = endPoint.getPath().split("/");
			StringBuilder urlPath = new StringBuilder("");
			if(pathElements != null){
				for(int i=0; i < pathElements.length; i++){
					String pathElement  = pathElements[i];
					if(pathElement != null && pathElement.length() > 0) {
						int position = pathElement.indexOf("{");
						if(urlPath.length() > 0) {
							urlPath.append("+");
						}
						if(position < 0) {
							urlPath.append("\"/"+pathElement+"\"");
						}else if (position == 0) {
							urlPath.append("\"/\"+"+pathElement.substring(1, pathElement.length()-1));
						}else{
							urlPath.append("\"/"+pathElement.replace("{format}", "json")+"\"");
						}
					}
				}
			}
			method.setResourcePath(endPoint.getPath());
			method.setName(nameGenerator.getMethodName(endPoint.getPath(), this.getSuggestedName()));
			
			//create method argument
			/**
			 * 1. API token need not be included as that is always added to the calls as HTTP headers
			 * 2. We need to handle auth token specially, hence need to differentiate that
			 * 3. Query parameters needs to be added as query string hence need to separate them out
			 * 4. Post parameters are usually WordnikObjects, hence we need to handle them separately 
			 */
			List<String> argNames = new ArrayList<String>();
			if(this.getParameters() != null) {
				List<MethodArgument> arguments = new ArrayList<MethodArgument>();
				List<MethodArgument> queryParams= new ArrayList<MethodArgument>();
				List<MethodArgument> pathParams= new ArrayList<MethodArgument>();
				method.setArguments(arguments);
				method.setQueryParameters(queryParams);
				method.setPathParameters(pathParams);
				
				for(ModelField modelField : this.getParameters()){
					if(!argNames.contains(modelField.getName())) {
						argNames.add(modelField.getName());
						MethodArgument anArgument = new MethodArgument();
						anArgument.setAllowedValues(modelField.getAllowedValuesString());
						//check if arguments has auth token
						if(modelField.getParamType().equalsIgnoreCase(PARAM_TYPE_HEADER) &&
								modelField.getName().equals(AUTH_TOKEN_PARAM_NAME)){
							method.setAuthToken(true);
							anArgument.setName(AUTH_TOKEN_ARGUMENT_NAME);
							anArgument.setDataType(MethodArgument.ARGUMENT_STRING);
							anArgument.setDescription(modelField.getDescription());
							arguments.add(anArgument);
						}else if(modelField.getParamType().equalsIgnoreCase(PARAM_TYPE_HEADER) &&
								modelField.getName().equals(API_KEY_PARAM_NAME)){
							//do nothing for API key parameter as all calls will automatically add API KEY to the http headers
						}else if (modelField.getParamType().equalsIgnoreCase(PARAM_TYPE_PATH) &&
								!modelField.getName().equalsIgnoreCase(FORMAT_PARAM_NAME)) {
							anArgument.setName(modelField.getName());
							anArgument.setDataType(MethodArgument.ARGUMENT_STRING);
							anArgument.setDescription(modelField.getDescription());
							arguments.add(anArgument);
							pathParams.add(anArgument);
						}else if (modelField.getParamType().equalsIgnoreCase(PARAM_TYPE_QUERY)) {
							anArgument.setName(modelField.getName());
							anArgument.setDataType(MethodArgument.ARGUMENT_STRING);
							anArgument.setDescription(modelField.getDescription());
							queryParams.add(anArgument);
							arguments.add(anArgument);
						}else if (modelField.getParamType().equalsIgnoreCase(PARAM_TYPE_BODY)) {
							if(modelField.getName() == null) {
								modelField.setName("postObject");
							}
							anArgument.setName(modelField.getName());
							anArgument.setDataType(dataTypeMapper.getReturnValueType(modelField.getDataType()));
							anArgument.setDescription(modelField.getDescription());
							arguments.add(anArgument);
                            method.setPostObject(true);
						}

                        if(modelField.isAllowMultiple() && dataTypeMapper.isPrimitiveType(modelField.getDataType())){
                            anArgument.setDataType(dataTypeMapper.getListReturnTypeSignature(
                                    dataTypeMapper.getReturnValueType(modelField.getDataType())));
                        }
                        anArgument.setInputModelClassArgument(inputobjectName, nameGenerator);
					}
				}
			}
			
			//check for number of arguments, if we have more than 4 then send the arguments as input object
			if(method.getArguments() != null && method.getArguments().size() > 4){
                List<MethodArgument> arguments = new ArrayList<MethodArgument>();
				Model modelforMethodInput = new Model();
				modelforMethodInput.setName(inputobjectName);
				List<ModelField> fields = new ArrayList<ModelField>();
				for(MethodArgument argument: method.getArguments()){
                    if(!argument.getName().equals("postObject") && !argument.getName().equals("authToken")){
                        ModelField aModelField = new ModelField();
                        aModelField.setAllowedValues(argument.getAllowedValues());
                        aModelField.setDescription(argument.getDescription());
                        aModelField.setName(argument.getName());
                        aModelField.setParamType(argument.getDataType());
                        fields.add(aModelField);
                    }else{
                        arguments.add(argument);
                    }
				}
				modelforMethodInput.setFields(fields);
				
				MethodArgument anArgument = new MethodArgument();
				anArgument.setDataType(inputobjectName);
				anArgument.setName(nameGenerator.applyMethodNamingPolicy(inputobjectName));
				arguments.add(anArgument);
				method.setArguments(arguments);
				method.setInputModel(modelforMethodInput);
			}
			
			List<String> argumentDefinitions = new ArrayList<String>();
			List<String> argumentNames = new ArrayList<String>();
            if (method.getArguments() != null && method.getArguments().size() > 0) {
                for(MethodArgument arg: method.getArguments()) {
                    if(!arg.getName().equalsIgnoreCase(FORMAT_PARAM_NAME)){
                        argumentDefinitions.add(arg.getDataType() + " " + arg.getName());
                        argumentNames.add(arg.getName());
                    }
                }
                method.setArgumentDefinitions(argumentDefinitions);
                method.setArgumentNames(argumentNames);
            }

            //get method type
			method.setMethodType(this.getHttpMethod());
			
			//get return value
			List<Response> response = this.getResponse();

			method.setReturnValue(dataTypeMapper.getReturnValueType(response.get(0).getValueType()));
			method.setReturnClassName(dataTypeMapper.getReturnClassType(response.get(0).getValueType()));

			
			//get description string for exception			
			method.setExceptionDescription(calculateExceptionMessage());
		}
		return method;
	} 	

	/**
	 * Each operation can have one or many error responses Concatenate all the error responses and create on string
	 * @return
	 */
	private String calculateExceptionMessage() {
		StringBuilder errorMessage = new StringBuilder();
		if(this.getResponse() != null) {
			for(Response response: this.getResponse()) {
				if(response.getErrorResponses() != null) {
					for(ErrorResponse errorResponse : response.getErrorResponses()){
						errorMessage.append(errorResponse.getCode() + " - " + errorResponse.getReason() +" ");
					}
				}
			}
		}
		return errorMessage.toString();
	}

}
