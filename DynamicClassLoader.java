package org.example;

import com.nutanix.sto.java.client.ApiClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.HashMap;

public class DynamicClassLoader {
    public static void main(String[] args) throws Exception {

        ApiClient client = new ApiClient();
        client.setHost("10.46.153.34"); // IPv4/IPv6 address or FQDN of the cluster
        client.setPort(9440); // Port to which to connect to
        client.setUsername("admin"); // UserName to connect to the cluster
        client.setPassword("Nutanix.123"); // Password to connect to the cluster}
        client.setVerifySsl(false);
        String filePath = "C:\\Users\\bhanuchandar.bagula\\storage-sdk-2\\src\\main\\java\\org\\example\\sdk_config.json";
        HashMap[] arg1 = new HashMap[1];

        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new InputStreamReader(new FileInputStream(filePath)));
            JSONObject jsonObject =  (JSONObject) obj;

            JSONObject storageJson = (JSONObject) jsonObject.get("storage_sdk");
            String apiModule = (String) storageJson.get("api_module");
            String payloadModule = (String) storageJson.get("payload_module");
            String responseModule = (String) storageJson.get("response_module");
            String listResponseModule = (String) storageJson.get("list_response_module");
            String taskResponseModule = (String) storageJson.get("task_response_module");

            Class<?> apiClass = Class.forName(apiModule);
            Constructor<?> constructorApi = apiClass.getConstructor(ApiClient.class);
            Object apiObj = constructorApi.newInstance(client);
            Class<?> resClass = Class.forName(responseModule);
            Class<?> lisResClass = Class.forName(listResponseModule);
            Class<?> taskResClass = Class.forName(taskResponseModule);

            // get by extID
            JSONObject getConfig = (JSONObject) storageJson.get("get");
            String getMethod = (String) getConfig.get("method");
            JSONArray getArgsType = (JSONArray) getConfig.get("arg_type");
            JSONObject getArgs = (JSONObject) getConfig.get("args");
            Class[] params = getArgTypeArray(getArgsType);
            String container_id = (String) getArgs.get("container_id");
            Method getStorageContainerByExtId = apiClass.getDeclaredMethod(getMethod, params);
            Object extIdResponse = resClass.cast(getStorageContainerByExtId.invoke(apiObj, container_id, arg1));
            Method getData = resClass.getDeclaredMethod("getData");
            Object getDataResponse = getData.invoke(extIdResponse);
            System.out.println(" : " + getDataResponse);

            // list
            JSONObject listConfig = (JSONObject) storageJson.get("list");
            String listMethod = (String) listConfig.get("method");
            JSONArray listArgsType = (JSONArray) listConfig.get("arg_type");
            JSONObject listArgs = (JSONObject) listConfig.get("args");
            Class[] listParams = getArgTypeArray(listArgsType);
            Integer page = ((Number) listArgs.get("page")).intValue();
            Integer limit = ((Number) listArgs.get("limit")).intValue();
            String filter = (String) listArgs.get("filter");
            String order_by = (String) listArgs.get("order_by");
            String select = (String) listArgs.get("select");
            Method getAllStorageContainers = apiClass.getDeclaredMethod(listMethod, listParams);
            Object list_response = lisResClass.cast(getAllStorageContainers.invoke
                    (apiObj,page,limit,filter,order_by,select,arg1));
            Method listData = lisResClass.getDeclaredMethod("getData");
            Object listDataResponse = listData.invoke(list_response);
            System.out.println(" : " + listDataResponse);

            // add storage container
            JSONObject createConfig = (JSONObject) storageJson.get("create");
            String createMethod = (String) createConfig.get("method");
            JSONObject createPayload = (JSONObject) createConfig.get("payload");
            JSONArray createArgsType = (JSONArray) createConfig.get("arg_type");
            Class[] createParams = getArgTypeArray(createArgsType);
            JSONObject createArgs = (JSONObject) createConfig.get("args");
            Class<?> payloadClass = Class.forName(payloadModule);
            Object payloadObj = payloadClass.newInstance();
            createParams[0] = payloadClass;
            for (Object o : createPayload.keySet()) {
                String key = (String) o;
                Method set = payloadClass.getDeclaredMethod(key, createPayload.get(key).getClass());
                set.invoke(payloadObj, createPayload.get(key));
            }
            String xClusterId = (String) createArgs.get("x_cluster_id");
            Method addStorageContainerForCluster = apiClass.getDeclaredMethod(createMethod, createParams);
            Object add_response = taskResClass.cast(addStorageContainerForCluster.invoke(apiObj, payloadObj, xClusterId, arg1));
            Method addData = taskResClass.getDeclaredMethod("getData");
            Object addDataResponse = addData.invoke(add_response);
            System.out.println(" : " + addDataResponse);

            // delete storage container
            JSONObject delConfig = (JSONObject) storageJson.get("delete");
            String delMethod = (String) delConfig.get("method");
            JSONArray delArgsType = (JSONArray) delConfig.get("arg_type");
            JSONObject delArgs = (JSONObject) delConfig.get("args");
            Class[] delParams = getArgTypeArray(delArgsType);
            String delContainerId = (String) delArgs.get("container_id");
            Boolean ignoreSmallFiles = (Boolean) delArgs.get("ignore_small_file");
            Method deleteStorageContainerByExtId = apiClass.getDeclaredMethod(delMethod, delParams);
            Object del_response = taskResClass.cast(deleteStorageContainerByExtId.invoke(apiObj,
                    delContainerId, ignoreSmallFiles, arg1));
            Method delData = taskResClass.getDeclaredMethod("getData");
            Object delDataResponse = delData.invoke(del_response);
            System.out.println(" : " + delDataResponse);


        } catch (ParseException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }
    public static Class[] getArgTypeArray(final JSONArray getArgsType){
        Class[] params = new Class[getArgsType.size()];
        for(int i=0; i<getArgsType.size(); i++){
            if (getArgsType.get(i).equals("int")){
                params[i] = Integer.class;
            }
            else if (getArgsType.get(i).equals("str")){
                params[i] = String.class;
                }
            else if (getArgsType.get(i).equals("bool")){
                params[i] = Boolean.class;
            }
            else{
                params[i] = java.util.HashMap[].class;
            }
        }
        return params;
    }
}