package org.example;

import com.nutanix.dp1.vmm.vmm.v4.ahv.config.ClusterReference;
//import javafx.event.EventTarget;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;


public class DynamicClassLoader {
    public static void main(String[] args) throws Exception {

        String filePath = "/Users/bhanuchandar.bagula/IdeaProjects/sdk-testing/src/test/java/org/example/sdk_config.json";
        HashMap[] arg1 = new HashMap[1];
//        HashMap<String, Object> arg1 = new HashMap<>();
//        arg1.put("extid", "uhnom");

        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(new InputStreamReader(new FileInputStream(filePath)));
            JSONObject jsonObject =  (JSONObject) obj;

            // API client object creation
            JSONObject storageJson = (JSONObject) jsonObject.get("storage_sdk");
            String apiClientModule = (String) storageJson.get("api_client_module");
            System.out.println("api client module : " + apiClientModule);
            Class<?> clientClass = Class.forName(apiClientModule);
            Object client = clientClass.newInstance();
            System.out.println("API client instance crated");
            Method setHost = clientClass.getDeclaredMethod("setHost", String.class);
            System.out.println("Setting host");
            setHost.invoke(client, "10.36.240.245");
            Method setPort = clientClass.getDeclaredMethod("setPort", int.class);
            int port = 9440;
            System.out.println("Setting port");
            setPort.invoke(client, port);
            System.out.println("Setting username & password");
            Method setUser = clientClass.getDeclaredMethod("setUsername", String.class);
            setUser.invoke(client, "admin");
            Method setPassword = clientClass.getDeclaredMethod("setPassword", String.class);
            setPassword.invoke(client, "Nutanix.123");
            Method setVerifySsl = clientClass.getDeclaredMethod("setVerifySsl", boolean.class);
            setVerifySsl.invoke(client, false);

            String apiModule = (String) storageJson.get("api_module");
            String payloadModule = (String) storageJson.get("payload_module");
            String responseModule = (String) storageJson.get("response_module");
            String listResponseModule = (String) storageJson.get("list_response_module");
            String taskResponseModule = (String) storageJson.get("task_response_module");
            String delResponseModule = (String) storageJson.get("delete_response_module");
            String createResponseModule = (String) storageJson.get("create_response_module");
            String updateResponseModule = (String) storageJson.get("update_response_module");

            Class<?> apiClass = Class.forName(apiModule);
            Constructor<?> constructorApi = apiClass.getConstructor(clientClass);
            Object apiObj = constructorApi.newInstance(client);
            Class<?> resClass = Class.forName(responseModule);
            Class<?> lisResClass = Class.forName(listResponseModule);
            Class<?> delResClass = Class.forName(delResponseModule);
            Class<?> createResClass = Class.forName(createResponseModule);
            Class<?> updateResClass = Class.forName(updateResponseModule);

            // list API call
            System.out.println("Firing list API");
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
                    (apiObj,page,limit,filter,select,order_by,arg1));
            Method listData = lisResClass.getDeclaredMethod("getData");
            Object listDataResponse = listData.invoke(list_response);
            System.out.println("List Response : " + listDataResponse);

            // get API call
            System.out.println("Firing GET API");
            JSONObject getConfig = (JSONObject) storageJson.get("get");
            String getMethod = (String) getConfig.get("method");
            JSONArray getArgsType = (JSONArray) getConfig.get("arg_type");
            JSONObject getArgs = (JSONObject) getConfig.get("args");
            Class[] params = getArgTypeArray(getArgsType);
            String ext_id = (String) getArgs.get("ext_id");
            Method getStorageContainerByExtId = apiClass.getDeclaredMethod(getMethod, params);
            Object extIdResponse = resClass.cast(getStorageContainerByExtId.invoke(apiObj, ext_id, arg1));
            Method getData = resClass.getDeclaredMethod("getData");
            Object getDataResponse = getData.invoke(extIdResponse);
            System.out.println("Get Response : " + getDataResponse);
            Method getReseved = resClass.getDeclaredMethod("get$reserved");
            Object getReservedResponse = getReseved.invoke(extIdResponse);
            int first = getReservedResponse.toString().indexOf("ETag=") + 5;
            int last = getReservedResponse.toString().length() - 1;
            String Etag = getReservedResponse.toString().substring(first, last);
            System.out.println("Etag : " + Etag);

            // create API call
            JSONObject createConfig = (JSONObject) storageJson.get("create");
            String createMethod = (String) createConfig.get("method");
            JSONObject createPayload = (JSONObject) createConfig.get("payload");
            JSONObject createPayloadArgType = (JSONObject) createConfig.get("payload_arg_type");
            JSONArray createArgsType = (JSONArray) createConfig.get("arg_type");
            Class[] createParams = getArgTypeArray(createArgsType);
            JSONObject createArgs = (JSONObject) createConfig.get("args");
            Class<?> payloadClass = Class.forName(payloadModule);
            Object payloadObj = payloadClass.newInstance();
            createParams[0] = payloadClass;
            for (Object o : createPayload.keySet()) {
                String key = (String) o;
                if (createPayloadArgType.get(key).equals("int")){
                    Integer arg = ((Number) createPayload.get(key)).intValue();
                    Method set = payloadClass.getDeclaredMethod(key, Integer.class);
                    set.invoke(payloadObj, arg);
                }
                else if (createPayloadArgType.get(key).equals("cluster_reference")) {
                    String cluster_id = (String) createPayload.get(key);
                    ClusterReference clusterReference = new ClusterReference();
                    clusterReference.setExtId(cluster_id);
                    Method set = payloadClass.getDeclaredMethod(key, clusterReference.getClass());
                    set.invoke(payloadObj, clusterReference);
                }
                else{
                Method set = payloadClass.getDeclaredMethod(key, createPayload.get(key).getClass());
                set.invoke(payloadObj, createPayload.get(key));
                }
            }
            String xClusterId = (String) createArgs.get("x_cluster_id");
            Method addStorageContainerForCluster = apiClass.getDeclaredMethod(createMethod, createParams);
            System.out.println("Firing Create API");
            Object add_response = createResClass.cast(addStorageContainerForCluster.invoke(apiObj, payloadObj, xClusterId, arg1));
            Method addData = createResClass.getDeclaredMethod("getData");
            Object addDataResponse = addData.invoke(add_response);
            System.out.println("Create Response : " + addDataResponse);

            // update API call
            JSONObject updateConfig = (JSONObject) storageJson.get("update");
            String updateMethod = (String) updateConfig.get("method");
            JSONObject updatePayload = (JSONObject) updateConfig.get("payload");
            JSONObject updatePayloadArgType = (JSONObject) updateConfig.get("payload_arg_type");
            JSONArray updateArgsType = (JSONArray) updateConfig.get("arg_type");
            Class[] updateParams = getArgTypeArray(updateArgsType);
            JSONObject updateArgs = (JSONObject) updateConfig.get("args");
            Class<?> payloadClassUpdate = Class.forName(payloadModule);
            Object payloadObjUpdate = payloadClassUpdate.newInstance();
            updateParams[0] = String.class;
            updateParams[1] = payloadClassUpdate;
            for (Object o : updatePayload.keySet()) {
                String key = (String) o;
                if (updatePayloadArgType.get(key).equals("int")){
                    Integer arg = ((Number) updatePayload.get(key)).intValue();
                    Method set = payloadClassUpdate.getDeclaredMethod(key, Integer.class);
                    set.invoke(payloadObjUpdate, arg);
                }
                else{
                    Method set = payloadClassUpdate.getDeclaredMethod(key, updatePayload.get(key).getClass());
                    set.invoke(payloadObjUpdate, updatePayload.get(key));
                }
            }
            String containerIdUpdate = (String) updateArgs.get("ext_id");
            Method updateStorageContainerForCluster = apiClass.getDeclaredMethod(updateMethod, updateParams);
            System.out.println("Firing Update API");
            arg1[0] = new HashMap<String, Object>();
            arg1[0].put("If-Match", Etag);
            Object update_response = updateResClass.cast(updateStorageContainerForCluster.invoke(apiObj, containerIdUpdate, payloadObjUpdate, arg1));
            Method updateData = updateResClass.getDeclaredMethod("getData");
            Object updateDataResponse = updateData.invoke(update_response);
            System.out.println("Update Response : " + updateDataResponse);

            // delete API call
            JSONObject delConfig = (JSONObject) storageJson.get("delete");
            String delMethod = (String) delConfig.get("method");
            JSONArray delArgsType = (JSONArray) delConfig.get("arg_type");
            JSONObject delArgs = (JSONObject) delConfig.get("args");
            Class[] delParams = getArgTypeArray(delArgsType);
            String delContainerId = (String) delArgs.get("container_id");
            Boolean ignoreSmallFiles = (Boolean) delArgs.get("ignore_small_file");
            Method deleteStorageContainerByExtId = apiClass.getDeclaredMethod(delMethod, delParams);
            System.out.println("Firing Delete API");
            arg1[0] = new HashMap<String, Object>();
            arg1[0].put("If-Match", Etag);
            Object del_response = delResClass.cast(deleteStorageContainerByExtId.invoke(apiObj,
                    delContainerId, ignoreSmallFiles, arg1));
            Method delData = delResClass.getDeclaredMethod("getData");
            Object delDataResponse = delData.invoke(del_response);
            System.out.println("Delete Response : " + delDataResponse);


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
                params[i] = HashMap[].class;
            }
        }
        return params;
    }
}
