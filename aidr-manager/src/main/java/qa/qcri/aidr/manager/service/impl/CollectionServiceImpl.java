package qa.qcri.aidr.manager.service.impl;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import qa.qcri.aidr.common.code.JacksonWrapper;
import qa.qcri.aidr.manager.dto.FetcheResponseDTO;
import qa.qcri.aidr.manager.dto.FetcherRequestDTO;
import qa.qcri.aidr.manager.dto.PingResponse;
import qa.qcri.aidr.manager.exception.AidrException;
import qa.qcri.aidr.manager.hibernateEntities.AidrCollection;
import qa.qcri.aidr.manager.hibernateEntities.AidrCollectionLog;
import qa.qcri.aidr.manager.hibernateEntities.UserConnection;
import qa.qcri.aidr.manager.hibernateEntities.UserEntity;
import qa.qcri.aidr.manager.repository.AuthenticateTokenRepository;
import qa.qcri.aidr.manager.repository.CollectionLogRepository;
import qa.qcri.aidr.manager.repository.CollectionRepository;
import qa.qcri.aidr.manager.repository.UserConnectionRepository;
import qa.qcri.aidr.manager.service.CollectionService;
import qa.qcri.aidr.manager.util.CollectionStatus;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import static qa.qcri.aidr.manager.util.CollectionType.SMS;
import static qa.qcri.aidr.manager.util.CollectionType.Twitter;

//import com.sun.jersey.api.client.Client;		// gf 3 way
//import com.sun.jersey.api.client.ClientResponse;
//import com.sun.jersey.api.client.WebResource;

@Service("collectionService")
public class CollectionServiceImpl implements CollectionService {

	private Logger logger = Logger.getLogger(getClass());
	@Autowired
	private CollectionRepository collectionRepository;
	@Autowired
	private CollectionLogRepository collectionLogRepository;
	@Autowired
	private UserConnectionRepository userConnectionRepository;

	@Autowired
	private AuthenticateTokenRepository authenticateTokenRepository;

	//@Autowired	// gf 3 way
	//private Client client;
	//private Client client = ClientBuilder.newClient();
	@Value("${fetchMainUrl}")
	private String fetchMainUrl;
	@Value("${twitter.consumerKey}")
	private String consumerKey;
	@Value("${twitter.consumerSecret}")
	private String consumerSecret;

	@Override
	@Transactional(readOnly = false)
	public void update(AidrCollection collection) throws Exception {
		collectionRepository.update(collection);
	}

	@Override
	@Transactional(readOnly = false)
	public void delete(AidrCollection collection) throws Exception {
		collectionRepository.delete(collection);

	}

	@Override
	@Transactional
	public void create(AidrCollection collection) throws Exception {
		collectionRepository.save(collection);
	}

	//  this method is common to get collection and should not filter by status
	@Override
	@Transactional(readOnly = true)
	public AidrCollection findById(Integer id) throws Exception {
		return collectionRepository.findById(id);
	}

	//  this method is common to get collection and should not filter by status
	@Override
	@Transactional(readOnly = true)
	public AidrCollection findByCode(String code) throws Exception {
		return collectionRepository.findByCode(code);
	}

	@Override
	@Transactional(readOnly = true)
	public AidrCollection findTrashedById(Integer id) throws Exception {
		AidrCollection temp = collectionRepository.findById(id);
		if (temp.getStatus().equals(CollectionStatus.TRASHED)) {
			return temp;
		}
		return null;

	}

	@Override
	@Transactional(readOnly = true)
	public AidrCollection findTrashedByCode(String code) throws Exception {
		AidrCollection temp = collectionRepository.findByCode(code);
		if (temp.getStatus().equals(CollectionStatus.TRASHED)) {
			return temp;
		}
		return null;

	}

	@Override
	@Transactional(readOnly = true)
	public List<AidrCollection> findAll(Integer start, Integer limit, UserEntity user, boolean onlyTrashed) throws Exception {
		return collectionRepository.getPaginatedData(start, limit, user, onlyTrashed);
	}

	@Override
	@Transactional(readOnly = true)
	public List<AidrCollection> findAllForPublic(Integer start, Integer limit, Enum statusValue) throws Exception {
		//logger.info("statusValue: " + statusValue);
		return collectionRepository.getPaginatedDataForPublic(start, limit, statusValue);
	}


	//    @Override
	//    @Transactional(readOnly = true)
	//    public CollectionDataResponse findAll(Integer start, Integer limit, Integer userId) throws Exception {
	//        return collectionRepository.getPaginatedData(start, limit, userId);
	//    }

	@Override
	@Transactional(readOnly = true)
	public List<AidrCollection> searchByName(String query, Integer userId) throws Exception {
		return collectionRepository.searchByName(query, userId);
	}

	@Override
	@Transactional(readOnly = true)
	public Boolean exist(String code) throws Exception {
		return collectionRepository.exist(code);
	}

	@Override
	@Transactional(readOnly = true)
	public Boolean existName(String name) throws Exception {
		return collectionRepository.existName(name);
	}

	@Override
	@Transactional(readOnly = true)
	public AidrCollection getRunningCollectionStatusByUser(Integer userId) throws Exception {
		return collectionRepository.getRunningCollectionStatusByUser(userId);
	}

	@Override
	@Transactional(readOnly = false)
	public AidrCollection updateAndGetRunningCollectionStatusByUser(Integer userId) throws Exception {
		AidrCollection collection = collectionRepository.getRunningCollectionStatusByUser(userId);
		if (collection != null){
			logger.info("User with ID: '" + userId + "' has a running collection with code: '" + collection.getCode() + "'. Trying to update collection from fetcher." );
			return statusByCollection(collection);
		} else {
			logger.info("User with ID: '" + userId + "' don't have any running collections. Nothing to update." );
			//            If there is no running collection there is still can be collection with status 'Initializing'.
			//            This happens because we update collection information from fetcher before collection was started.
			//            So we need to update from Fetcher this kind of collections as well.
			collection = collectionRepository.getInitializingCollectionStatusByUser(userId);
			if (collection != null) {
				return statusByCollection(collection);
			}
		}
		return null;
	}

	@Override
	@Transactional(readOnly = false)
	public AidrCollection start(Integer collectionId) throws Exception {

		// We are going to start new collection. Lets stop collection which is running for owner of the new collection.
		AidrCollection dbCollection = collectionRepository.findById(collectionId);
		Integer userId = dbCollection.getUser().getId();
		AidrCollection alreadyRunningCollection = collectionRepository.getRunningCollectionStatusByUser(userId);
		if (alreadyRunningCollection != null) {
			this.stop(alreadyRunningCollection.getId());
		}

		return startFetcher(prepareFetcherRequest(dbCollection), dbCollection);
	}



	//    @Override
	//    @Transactional(readOnly = false)
	//    public AidrCollection start(Integer collectionId, Integer userId) throws Exception {
	//        AidrCollection alreadyRunningCollection = collectionRepository.getRunningCollectionStatusByUser(userId);
	//        if (alreadyRunningCollection != null) {
	//            this.stop(alreadyRunningCollection.getId());
	//        }
	//        AidrCollection dbCollection = collectionRepository.findById(collectionId);
	//        return startFetcher(prepareFetcherRequest(dbCollection), dbCollection);
	//    }

	@Transactional(readOnly = true)
	public FetcherRequestDTO prepareFetcherRequest(AidrCollection dbCollection) {
		FetcherRequestDTO dto = new FetcherRequestDTO();

		UserConnection userconnection = userConnectionRepository.fetchbyUsername(dbCollection.getUser().getUserName());
		dto.setAccessToken(userconnection.getAccessToken());
		dto.setAccessTokenSecret(userconnection.getSecret());
		dto.setConsumerKey(consumerKey);
		dto.setConsumerSecret(consumerSecret);
		dto.setCollectionName(dbCollection.getName());
		dto.setCollectionCode(dbCollection.getCode());
		dto.setToFollow(dbCollection.getFollow());
		dto.setToTrack(dbCollection.getTrack());
		dto.setGeoLocation(dbCollection.getGeo());
		dto.setLanguageFilter(dbCollection.getLangFilters());
		return dto;
	}

	@Override
	@Transactional(readOnly = false)
	public AidrCollection stop(Integer collectionId) throws Exception {
		AidrCollection collection = collectionRepository.findById(collectionId);
		AidrCollection updateCollection = stopAidrFetcher(collection);

		AidrCollectionLog collectionLog = new AidrCollectionLog();
		collectionLog.setCount(collection.getCount());
		collectionLog.setEndDate(collection.getEndDate());
		collectionLog.setFollow(collection.getFollow());
		collectionLog.setGeo(collection.getGeo());
		collectionLog.setLangFilters(collection.getLangFilters());
		collectionLog.setStartDate(collection.getStartDate());
		collectionLog.setTrack(collection.getTrack());
		collectionLog.setCollectionID(collectionId);
		collectionLogRepository.save(collectionLog);

		return updateCollection;
	}

	public AidrCollection startFetcher(FetcherRequestDTO fetcherRequest, AidrCollection aidrCollection) {
		try {
			/**
			 * Rest call to Fetcher
			 */
			 Client client = ClientBuilder.newBuilder().register(JacksonFeature.class).build();
			 // gf 3 way
			 if (aidrCollection.getCollectionType() == Twitter) {
				 WebTarget webResource = client.target(fetchMainUrl + "/twitter/start");

				 System.out.println("In startFetcher...");
				 ObjectMapper objectMapper = JacksonWrapper.getObjectMapper();

				 Response clientResponse = webResource.request(MediaType.APPLICATION_JSON)
						 .post(Entity.json(objectMapper.writeValueAsString(fetcherRequest)), Response.class);

				 System.out.println("ObjectMapper: " + objectMapper.writeValueAsString(fetcherRequest));
				 System.out.println("Response = " + clientResponse);

				 String jsonResponse = clientResponse.readEntity(String.class);

				 logger.info("NEW STRING: " + jsonResponse);
				 FetcheResponseDTO response = objectMapper.readValue(jsonResponse, FetcheResponseDTO.class);
				 logger.info("start Response from fetchMain " + objectMapper.writeValueAsString(response));
				 aidrCollection.setStatus(CollectionStatus.getByStatus(response.getStatusCode()));
			 } else if (aidrCollection.getCollectionType() == SMS){
				 WebTarget webResource = client.target(fetchMainUrl + "/sms/start?collection_code=" + URLEncoder.encode(aidrCollection.getCode(), "UTF-8"));
				 Response response = webResource.request(MediaType.APPLICATION_JSON).get();
				 if (response.getStatus() == 200)
					 aidrCollection.setStatus(CollectionStatus.RUNNING);
			 }
			 /**
			  * Update Status To database
			  */
			 collectionRepository.update(aidrCollection);
			 return aidrCollection;
		} catch (Exception e) {
			logger.error("Error while starting Remote FetchMain Collection", e);
		}
		return null;
	}

	@Override
	public boolean pingCollector() throws AidrException {
		try {
			Client client = ClientBuilder.newBuilder().register(JacksonFeature.class).build();
			WebTarget webResource = client.target(fetchMainUrl + "/manage/ping");

			ObjectMapper objectMapper = JacksonWrapper.getObjectMapper();
			Response clientResponse = webResource.request(MediaType.APPLICATION_JSON).get();

			String jsonResponse = clientResponse.readEntity(String.class);

			PingResponse pingResponse = objectMapper.readValue(jsonResponse, PingResponse.class);
			if (pingResponse != null && "RUNNING".equals(pingResponse.getCurrentStatus())) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			throw new AidrException("Error while Getting training data for Crisis and Model.", e);
		}
	}

	@SuppressWarnings("deprecation")
	public AidrCollection stopAidrFetcher(AidrCollection collection) {
		try {
			/**
			 * Rest call to Fetcher
			 */
			Client client = ClientBuilder.newBuilder().register(JacksonFeature.class).build();
			String path = "";
			if (collection.getCollectionType() == Twitter) {
				path = "/twitter/stop?id=";
			} else if(collection.getCollectionType() == SMS){
				path = "/sms/stop?collection_code=";
			}

			WebTarget webResource = client.target(fetchMainUrl + path + URLEncoder.encode(collection.getCode(), "UTF-8"));

			Response clientResponse = webResource.request(MediaType.APPLICATION_JSON).get();

			String jsonResponse = clientResponse.readEntity(String.class);

			collection = updateStatusCollection(jsonResponse, collection);

			/**
			 * Change Database Status
			 */
			return this.collectionRepository.stop(collection.getId());
		} catch (Exception e) {
			logger.error("Error while stopping Remote FetchMain Collection", e);
		}
		return null;
	}

	private AidrCollection updateStatusCollection(String jsonResponse, AidrCollection collection) throws IOException {
		ObjectMapper objectMapper = JacksonWrapper.getObjectMapper();
		FetcheResponseDTO response = objectMapper.readValue(jsonResponse, FetcheResponseDTO.class);
		if (response != null) {
			if (!CollectionStatus.getByStatus(response.getStatusCode()).equals(collection.getStatus())) {
				//if local=running and fetcher=NOT-FOUND then put local as NOT-RUNNING
				if (CollectionStatus.NOT_FOUND.equals(CollectionStatus.getByStatus(response.getStatusCode()))) {
					collection.setStatus(CollectionStatus.NOT_RUNNING);
					collectionRepository.update(collection);
				}

				if (CollectionStatus.RUNNING.equals(CollectionStatus.getByStatus(response.getStatusCode()))) {
					collection = collectionRepository.start(collection.getId());
				}
			}

			if (response.getCollectionCount() != null && !response.getCollectionCount().equals(collection.getCount())) {
				collection.setCount(response.getCollectionCount());
				String lastDocument = response.getLastDocument();
				if (lastDocument != null)
					collection.setLastDocument(lastDocument);
				collectionRepository.update(collection);
			}
		}
		return collection;
	}

	@SuppressWarnings("deprecation")
	@Override
	public AidrCollection statusById(Integer id) throws Exception {
		AidrCollection collection = this.findById(id);
		return statusByCollection(collection);
	}

	@SuppressWarnings("deprecation")
	@Override
	public AidrCollection statusByCollection(AidrCollection collection) throws Exception {
		if (collection != null) {
			try {
				/**
				 * Make a call to fetcher Status Rest API
				 */
				Client client = ClientBuilder.newBuilder().register(JacksonFeature.class).build();

				String path = "";
				if (collection.getCollectionType() == Twitter) {
					path = "/twitter/status?id=";
				} else if(collection.getCollectionType() == SMS){
					path = "/sms/status?collection_code=";
				}

				WebTarget webResource = client.target(fetchMainUrl + path + URLEncoder.encode(collection.getCode(), "UTF-8"));
				Response clientResponse = webResource.request(MediaType.APPLICATION_JSON).get();

				String jsonResponse = clientResponse.readEntity(String.class);

				collection = updateStatusCollection(jsonResponse, collection);
				return collection;
			} catch (Exception e) {
				String msg = "Error while getting status for collection from Remote FetchMain Collection";
				logger.error(msg, e);
				throw new Exception(msg);
			}
		}
		return null;
	}

	@Override
	@Transactional(readOnly = true)
	public List<AidrCollection> getRunningCollections() throws Exception {
		return collectionRepository.getRunningCollections();
	}

	@Override
	@Transactional(readOnly = true)
	public List<AidrCollection> getRunningCollections(Integer start, Integer limit, String terms, String sortColumn, String sortDirection) throws Exception {
		return collectionRepository.getRunningCollections(start, limit, terms, sortColumn, sortDirection);
	}

	@Override
	@Transactional(readOnly = true)
	public Long getRunningCollectionsCount(String terms) throws Exception {
		return collectionRepository.getRunningCollectionsCount(terms);
	}

	@Override
	@Transactional(readOnly = true)
	public List<AidrCollection> getStoppedCollections(Integer start, Integer limit, String terms, String sortColumn, String sortDirection) throws Exception {
		return collectionRepository.getStoppedCollections(start, limit, terms, sortColumn, sortDirection);
	}

	@Override
	@Transactional(readOnly = true)
	public Long getStoppedCollectionsCount(String terms) throws Exception {
		return collectionRepository.getStoppedCollectionsCount(terms);
	}

	@Override
	@Transactional(readOnly = true)
	public Integer getCollectionsCount(UserEntity user, boolean onlyTrashed) throws Exception {
		return collectionRepository.getCollectionsCount(user, onlyTrashed);
	}

	@Override
	@Transactional(readOnly = true)
	public Integer getPublicCollectionsCount(Enum statusValue) throws Exception {
		return collectionRepository.getPublicCollectionsCount(statusValue);
	}

	@Override
	@Transactional(readOnly = true)
	public Boolean isValidToken(String token) throws Exception {
		return authenticateTokenRepository.isAuthorized(token);
	}

	@Override
	@Transactional(readOnly = true)
	public List<AidrCollection> geAllCollectionByUser(Integer userId) throws Exception{
		return collectionRepository.getAllCollectionByUser(userId);
	}
}
