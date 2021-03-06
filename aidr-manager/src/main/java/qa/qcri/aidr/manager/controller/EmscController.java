package qa.qcri.aidr.manager.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import qa.qcri.aidr.manager.hibernateEntities.AidrCollection;
import qa.qcri.aidr.manager.hibernateEntities.UserEntity;
import qa.qcri.aidr.manager.service.CollectionService;
import qa.qcri.aidr.manager.util.CollectionStatus;
import qa.qcri.aidr.manager.util.JsonDataValidator;
/**
 * @deprecated  replaced by {@link @PublicController}
 */
@Deprecated
@Controller
@RequestMapping("emsc/collection")
public class EmscController extends BaseController{

    private Logger logger = Logger.getLogger(getClass());

    @Autowired
    private CollectionService collectionService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }

    @RequestMapping(value = "/start.action", method = RequestMethod.POST)
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseBody
    public Map<String,Object> saveStart(@RequestBody final String jsonString) throws Exception {
       // logger.info("saveStart CeaController  : "+jsonString);
        try{
            if(jsonString == null){
                return getUIWrapper(false);
            }

            if(JsonDataValidator.isValidCeaJSON(jsonString)){
                JSONParser parser = new JSONParser();
                Object obj = parser.parse(jsonString);

                JSONObject jsonObject = (JSONObject) obj;
                String token = (String)jsonObject.get("token");

                if(collectionService.isValidToken(token)){
                    String code = (String) jsonObject.get("code");
                    String geo = (String) jsonObject.get("geo");
                    long defaultHours = (Long)jsonObject.get("durationInHours") ;
                    JSONArray adminAccount = (JSONArray) jsonObject.get("shareWithAccounts");

                    JSONArray account = (JSONArray) jsonObject.get("account");
                    UserEntity userEntity= userService.getAvailableUser(account) ;

                    if(userEntity != null){
                        List<UserEntity> adminEntities =  getAdminUsers(adminAccount, userEntity);
                        //logger.info("userEntity CeaController  : "+userEntity.getUserName());
                        List<AidrCollection> collectionList = collectionService.geAllCollectionByUser(userEntity.getId()) ;

                        if(collectionList != null && collectionList.size() > 0){
                            for(int i=0; i < collectionList.size() ; i++){
                                AidrCollection aCol = collectionList.get(i);
                                if(aCol.getStatus().equals(CollectionStatus.INITIALIZING) || aCol.getStatus().equals(CollectionStatus.RUNNING) ){
                                   // logger.info("collectionService.stop : " + aCol.getId());
                                    try{
                                        collectionService.stop(aCol.getId());
                                    }
                                    catch(Exception e){
                                        logger.error("Error while stop running collection", e);
                                        return getUIWrapper(e.getMessage(), false);
                                    }
                                }
                            }
                        }

                        AidrCollection collection = new AidrCollection();
                        collection.setUser(userEntity);
                        collection.setStatus(CollectionStatus.NOT_RUNNING);
                        collection.setPubliclyListed(true);
                        Calendar now = Calendar.getInstance();
                        collection.setCreatedDate(now.getTime());

                        String name = code;
                        code = code+ now.getTime().hashCode();
                        logger.info("updated code : " + code);
                        collection.setCode(code);
                        collection.setGeo(geo);
                        collection.setName(name);
                        collection.setDurationHours((int)defaultHours);
                        collection.setLangFilters("");
                        collection.setManagers(adminEntities);

                        try{
                            collectionService.create(collection);
                            collectionService.start(collection.getId());

                            return getUIWrapper("successful", true, Long.parseLong("1"), "successful");
                        }
                        catch(Exception e){
                            logger.error("failing to create or start collection", e);
                            return getUIWrapper(e.getMessage(), false);
                        }

                    }
                    else{
                        return getUIWrapper("bad user account info", false);
                    }
                }
                else{
                    return getUIWrapper("Invalid token", false);
                }
            }
            return getUIWrapper("bad json", false);

        }catch(Exception e){
            logger.error("Error while saveStart AidrCollection. unknown issues....", e);
            return getUIWrapper(e.getMessage(), false);
        }
    }

    private List<UserEntity> getAdminUsers(JSONArray adminAccounts, UserEntity owneraccount){
        List<UserEntity> adminUsers = new ArrayList<UserEntity>();
        adminUsers.add(owneraccount);
        for(int i = 0; i < adminAccounts.size(); i++){
            String fetchedName = (String)adminAccounts.get(i);
            UserEntity userEntity= userService.fetchByUserName(fetchedName) ;
            if(userEntity != null){
                if(!userEntity.getUserName().equalsIgnoreCase(owneraccount.getUserName())) {
                    adminUsers.add(userEntity);
                }
            }
        }

        return adminUsers;
    }



}
