package qa.qcri.aidr.manager.controller;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import qa.qcri.aidr.manager.dto.TaggerCrisis;
import qa.qcri.aidr.manager.dto.TaggerModel;
import qa.qcri.aidr.manager.hibernateEntities.AidrCollection;
import qa.qcri.aidr.manager.hibernateEntities.UserEntity;
import qa.qcri.aidr.manager.service.CollectionLogService;
import qa.qcri.aidr.manager.service.CollectionService;
import qa.qcri.aidr.manager.service.TaggerService;
import qa.qcri.aidr.manager.util.CollectionType;

import java.util.List;
import java.util.Map;

import static qa.qcri.aidr.manager.util.CollectionStatus.RUNNING;
import static qa.qcri.aidr.manager.util.CollectionStatus.RUNNING_WARNING;


@Controller
public class ScreenController extends BaseController{

    @Autowired
    private CollectionService collectionService;
    @Autowired
    private TaggerService taggerService;
    @Value("${fetchMainUrl}")
    private String fetchMainUrl;
    @Autowired
    private CollectionLogService collectionLogService;

	private Logger logger = Logger.getLogger(ScreenController.class);
    
	@RequestMapping("protected/home")
	public ModelAndView home() throws Exception {
        String userName = getAuthenticatedUserName();

        ModelAndView model = new ModelAndView("home");
        model.addObject("userName", userName);
        model.addObject("collectionTypes", CollectionType.JSON());
        return model;
	}

	@RequestMapping("signin")
	public String signin(Map<String, String> model) throws Exception {
		return "signin";
	}

    @RequestMapping("protected/access-error")
    public ModelAndView accessError() throws Exception {
        return new ModelAndView("access-error");
    }

    private boolean isHasPermissionForCollection(String code) throws Exception{
        UserEntity user = getAuthenticatedUser();
        if (user == null){
            return false;
        }

//        current user is Admin
        if (userService.isUserAdmin(user)) {
            return true;
        }

        AidrCollection collection = collectionService.findByCode(code);
        if (collection == null){
            return false;
        }

//        current user is a owner of the collection
        if(user.getUserName().equals(collection.getUser().getUserName())){
            return true;
        }

//        current user is in managers list of the collection
        if (userService.isUserInCollectionManagersList(user, collection)){
            return true;
        }
        return false;
    }

    @RequestMapping("protected/{code}/collection-details")
    public ModelAndView collectionDetails(@PathVariable(value="code") String code) throws Exception {
        if (!isHasPermissionForCollection(code)){
            return new ModelAndView("redirect:/protected/access-error");
        }

        String userName = getAuthenticatedUserName();
        AidrCollection collection = collectionService.findByCode(code);

        ModelAndView model = new ModelAndView("collection-details");
        model.addObject("id", collection.getId());
        model.addObject("collectionCode", code);
        model.addObject("userName", userName);
        model.addObject("fetchMainUrl", fetchMainUrl);
        model.addObject("collectionType", collection.getCollectionType());
        model.addObject("collectionTypes", CollectionType.JSON());

        return model;
    }

    @RequestMapping("protected/collection-create")
    public ModelAndView collectionCreate() throws Exception {
        ModelAndView model = new ModelAndView("collection-create");

        String userName = getAuthenticatedUserName();
        model.addObject("collectionTypes", CollectionType.JSON());
        model.addObject("userName", userName);
        model.addObject("userId", getAuthenticatedUser().getId());

        return model;

    }

    @RequestMapping("protected/{code}/tagger-collection-details")
    public ModelAndView taggerCollectionDetails(@PathVariable(value="code") String code) throws Exception {
    	logger.info("Received request for crisis code = " + code);
    	if (!isHasPermissionForCollection(code)){
            logger.info("protected access-error");
    		return new ModelAndView("redirect:/protected/access-error");
        }

        TaggerCrisis crisis = taggerService.getCrisesByCode(code);
        logger.info("returned from getCrisesByCode");
        AidrCollection collection = collectionService.findByCode(code);
        logger.info("returned from findByCode");

        Integer crisisId = 0;
        String crisisName = "";
        Integer crisisTypeId = 0;
        Boolean isMicromapperEnabled = false;
        if (crisis != null && crisis.getCrisisID() != null && crisis.getName() != null){
            crisisId = crisis.getCrisisID();
            crisisName = crisis.getName();
            if (crisis.getCrisisType() != null && crisis.getCrisisType().getCrisisTypeID() != null){
                crisisTypeId = crisis.getCrisisType().getCrisisTypeID();
            }
            
        	isMicromapperEnabled = crisis.getIsMicromapperEnabled();
        }
        logger.info("Fetched tagger crisis: " + crisis.getCode() + ", aidr collection: " + collection.getCode());
        
        ModelAndView model = new ModelAndView("tagger/tagger-collection-details");
        model.addObject("crisisId", crisisId);
        model.addObject("name", crisisName);
        model.addObject("crisisTypeId", crisisTypeId);
        model.addObject("code", code);
        model.addObject("isMicromapperEnabled", isMicromapperEnabled);
        model.addObject("collectionType", collection.getCollectionType());
        model.addObject("collectionTypes", CollectionType.JSON());
        
        logger.info("Returning model: " + model.getModel());
        return model;
    }

    @RequestMapping("protected/{code}/predict-new-attribute")
    public ModelAndView predictNewAttribute(@PathVariable(value="code") String code) throws Exception {
        if (!isHasPermissionForCollection(code)){
            return new ModelAndView("redirect:/protected/access-error");
        }

        TaggerCrisis crisis = taggerService.getCrisesByCode(code);

        Integer crisisId = 0;
        String crisisName = "";
        if (crisis != null && crisis.getCrisisID() != null && crisis.getName() != null){
            crisisId = crisis.getCrisisID();
            crisisName = crisis.getName();
        }

        ModelAndView model = new ModelAndView("tagger/predict-new-attribute");
        model.addObject("crisisId", crisisId);
        model.addObject("name", crisisName);
        model.addObject("code", code);
        return model;
    }

    @RequestMapping("protected/{id}/attribute-details")
    public ModelAndView attributeDetails(@PathVariable(value="id") Integer id) throws Exception {
        ModelAndView model = new ModelAndView("tagger/attribute-details");
        Integer taggerUserId = 0;
        try {
            String userName = getAuthenticatedUserName();
            taggerUserId = taggerService.isUserExistsByUsername(userName);

        } catch (Exception e) {
            logger.error("Exception while getting attribute details",e);
        }
        model.addObject("id", id);
        model.addObject("userId", taggerUserId);
        return model;
    }

    @RequestMapping("protected/{code}/{id}/model-details")
    public ModelAndView modelDetails(@PathVariable(value="code") String code, @PathVariable(value="id") Integer modelId) throws Exception {
        if (!isHasPermissionForCollection(code)){
            return new ModelAndView("redirect:/protected/access-error");
        }

        TaggerCrisis crisis = taggerService.getCrisesByCode(code);

        Integer crisisId = 0;
        Integer modelFamilyId = 0;
        Integer attributeId = 0;
        String crisisName = "";
        String modelName = "";
        double modelAuc = 0;
        if (crisis != null && crisis.getCrisisID() != null && crisis.getName() != null){
            crisisId = crisis.getCrisisID();
            crisisName = crisis.getName();
        }

        List<TaggerModel> modelsForCrisis = taggerService.getModelsForCrisis(crisisId);
        for (TaggerModel model : modelsForCrisis) {
            if (modelId.equals(model.getModelID())){
                modelName = model.getAttribute();
                if (model.getModelFamilyID() != null) {
                    modelFamilyId = model.getModelFamilyID();
                }
                modelAuc = model.getAuc();
                attributeId = model.getAttributeID();
            }
        }

        Integer taggerUserId = 0;
        try {
            String userName = getAuthenticatedUserName();
            taggerUserId = taggerService.isUserExistsByUsername(userName);
            if(taggerUserId == null){
                taggerUserId = 0;
            }


        } catch (Exception e) {
           // System.out.println("e : " + e);
        	logger.error("Exception while checking whether user exist by username",e);
        }

        AidrCollection collection = collectionService.findByCode(code);

        ModelAndView model = new ModelAndView("tagger/model-details");
        model.addObject("crisisId", crisisId);
        model.addObject("crisisName", crisisName);
        model.addObject("modelName", modelName);
        model.addObject("modelId", modelId);
        model.addObject("modelAuc", modelAuc);
        model.addObject("modelFamilyId", modelFamilyId);
        model.addObject("code", code);
        model.addObject("userId", taggerUserId);
        model.addObject("attributeId", attributeId);
        model.addObject("collectionType", collection.getCollectionType());
        model.addObject("collectionTypes", CollectionType.JSON());

        return model;
    }

    @RequestMapping("protected/{code}/new-custom-attribute")
    public ModelAndView newCustomAttribute(@PathVariable(value="code") String code) throws Exception {
        if (!isHasPermissionForCollection(code)){
            return new ModelAndView("redirect:/protected/access-error");
        }

        TaggerCrisis crisis = taggerService.getCrisesByCode(code);
        Integer crisisId = 0;
        String crisisName = "";

        if (crisis != null && crisis.getCrisisID() != null && crisis.getName() != null){
            crisisId = crisis.getCrisisID();
            crisisName = crisis.getName();
        }

        AidrCollection collection = collectionService.findByCode(code);

        ModelAndView model = new ModelAndView("tagger/new-custom-attribute");
        model.addObject("code", code);
        model.addObject("crisisId", crisisId);
        model.addObject("crisisName", crisisName);
        model.addObject("collectionType", collection.getCollectionType());
        model.addObject("collectionTypes", CollectionType.JSON());

        return model;
    }

    @RequestMapping("protected/{code}/{modelId}/{modelFamilyId}/{attributeID}/training-data")
    public ModelAndView trainingData(@PathVariable(value="code") String code,
                                     @PathVariable(value="modelId") Integer modelId,
                                     @PathVariable(value="modelFamilyId") Integer modelFamilyId,
                                     @PathVariable(value="attributeID") Integer attributeID) throws Exception {
        if (!isHasPermissionForCollection(code)){
            return new ModelAndView("redirect:/protected/access-error");
        }

        TaggerCrisis crisis = taggerService.getCrisesByCode(code);

        Integer crisisId = 0;
        String crisisName = "";
        String modelName = "";
        double modelAuc = 0;
        long trainingExamples = 0;
        Integer retrainingThreshold = 50;
        if (crisis != null && crisis.getCrisisID() != null && crisis.getName() != null){
            crisisId = crisis.getCrisisID();
            crisisName = crisis.getName();

        }

        List<TaggerModel> modelsForCrisis = taggerService.getModelsForCrisis(crisisId);
        for (TaggerModel model : modelsForCrisis) {
            if (attributeID.equals(model.getAttributeID())){
                modelName = model.getAttribute();
                trainingExamples = model.getTrainingExamples();
                modelAuc = model.getAuc();
                retrainingThreshold = model.getRetrainingThreshold();
            }
        }

        AidrCollection collection = collectionService.findByCode(code);

        ModelAndView model = new ModelAndView("tagger/training-data");
        model.addObject("crisisId", crisisId);
        model.addObject("crisisName", crisisName);
        model.addObject("modelName", modelName);
        model.addObject("modelId", modelId);
        model.addObject("modelFamilyId", modelFamilyId);
        model.addObject("attributeID", attributeID);
        model.addObject("code", code);
        model.addObject("trainingExamples", trainingExamples);
        model.addObject("modelAuc", modelAuc);
        model.addObject("retrainingThreshold", retrainingThreshold);
        model.addObject("collectionType", collection.getCollectionType());
        model.addObject("collectionTypes", CollectionType.JSON());

        return model;
    }

    @RequestMapping("protected/{code}/{modelId}/{modelFamilyId}/{nominalAttributeId}/training-examples")
    public ModelAndView trainingExamples(@PathVariable(value="code") String code,
                                         @PathVariable(value="modelId") Integer modelId,
                                         @PathVariable(value="modelFamilyId") Integer modelFamilyId,
                                         @PathVariable(value="nominalAttributeId") Integer nominalAttributeId) throws Exception {
        if (!isHasPermissionForCollection(code)){
            return new ModelAndView("redirect:/protected/access-error");
        }

        TaggerCrisis crisis = taggerService.getCrisesByCode(code);

        Integer crisisId = 0;
        String crisisName = "";
        String modelName = "";
        if (crisis != null && crisis.getCrisisID() != null && crisis.getName() != null){
            crisisId = crisis.getCrisisID();
            crisisName = crisis.getName();
        }

        List<TaggerModel> modelsForCrisis = taggerService.getModelsForCrisis(crisisId);
        for (TaggerModel model : modelsForCrisis) {
            if (nominalAttributeId.equals(model.getAttributeID())){
                modelName = model.getAttribute();
            }
        }


        AidrCollection collection = collectionService.findByCode(code);

        ModelAndView model = new ModelAndView("tagger/training-examples");
        model.addObject("code", code);
        model.addObject("crisisId", crisisId);
        model.addObject("crisisName", crisisName);
        model.addObject("modelName", modelName);
        model.addObject("modelId", modelId);
        model.addObject("modelFamilyId", modelFamilyId);
        model.addObject("nominalAttributeId", nominalAttributeId);
        model.addObject("collectionType", collection.getCollectionType());
        model.addObject("collectionTypes", CollectionType.JSON());

        return model;
    }

    @RequestMapping("protected/administration/admin-console")
    public ModelAndView adminConsole(Map<String, String> model) throws Exception {
        UserEntity user = getAuthenticatedUser();
        if (!userService.isUserAdmin(user)){
            return new ModelAndView("redirect:/protected/access-error");
        }

        return new ModelAndView( "administration/admin-console");
    }

    @RequestMapping("protected/administration/admin-health")
    public ModelAndView adminHealth(Map<String, String> model) throws Exception {
        UserEntity user = getAuthenticatedUser();
        if (!userService.isUserAdmin(user)){
            return new ModelAndView("redirect:/protected/access-error");
        }

        return new ModelAndView("administration/health");
    }

    @RequestMapping("protected/{code}/interactive-view-download")
         public ModelAndView interactiveViewDownload(@PathVariable(value="code") String code) throws Exception {

        String userName ="";
       // System.out.println("interactiveViewDownload : ");

        if (isHasPermissionForCollection(code)){
            userName = getAuthenticatedUserName();
        }

        return getInteractiveViewDownload(code,userName);
    }

    @RequestMapping("public/{code}/interactive-view-download")
    public ModelAndView publicInteractiveViewDownload(@PathVariable(value="code") String code) throws Exception {
        return getInteractiveViewDownload(code, "");
    }

    @RequestMapping("public/{code}/{username}/interactive-view-download")
    public ModelAndView privateInteractiveViewDownload(@PathVariable(value="code") String code,
                                                       @PathVariable(value="username") String username) throws Exception {

        return getInteractiveViewDownload(code, username);
    }

    private ModelAndView getInteractiveViewDownload(String code, String userName){

        TaggerCrisis crisis = null;
        AidrCollection collection = null;
        try {
            crisis = taggerService.getCrisesByCode(code);
            collection = collectionService.findByCode(code);
        } catch (Exception e) {
        	logger.error("Exception while getting interactive view download", e);
        }

        Integer crisisId = 0;
        String crisisName = "";
        if (crisis != null && crisis.getCrisisID() != null && crisis.getName() != null){
            crisisId = crisis.getCrisisID();
            //crisisName = crisis.getName();
            crisisName = collection.getName();
        }

        Integer collectionCount = 0;
        Integer collectionId = 0;
        CollectionType type = CollectionType.Twitter;
        if (collection != null){
            if (collection.getId() != null) {
                collectionId = collection.getId();
                try {
                    collectionCount = collectionLogService.countTotalDownloadedItemsForCollection(collectionId);
                } catch (Exception e) {
                	logger.error("Exception while counting total download items for collectionID: "+collectionId, e);
                }
            }
            if (collection.getCount() != null && (collection.getStatus() != null || RUNNING == collection.getStatus() || RUNNING_WARNING == collection.getStatus())) {
                collectionCount += collection.getCount();
            }
            if (collection.getCollectionType() != null) {
                type = collection.getCollectionType();
            }
        }

        ModelAndView model = new ModelAndView("../public/interactive-view-download");
        model.addObject("collectionId", collectionId);
        model.addObject("crisisId", crisisId);
        model.addObject("crisisName", crisisName);
        model.addObject("code", code);
        model.addObject("count", collectionCount);
        model.addObject("userName", userName);
        model.addObject("collectionType", type);
        model.addObject("collectionTypes", CollectionType.JSON());

        return model;
    }

}
