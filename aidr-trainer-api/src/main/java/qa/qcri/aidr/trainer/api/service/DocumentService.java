package qa.qcri.aidr.trainer.api.service;

import qa.qcri.aidr.trainer.api.entity.Document;
import qa.qcri.aidr.trainer.api.template.TaskBufferJsonModel;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jilucas
 * Date: 10/1/13
 * Time: 12:48 PM
 * To change this template use File | Settings | File Templates.
 */
public interface DocumentService {

    void updateHasHumanLabel(Long documentID, boolean value);
    Document findDocument(Long documentID);
    List<Document> getDocumentForTask(Long crisisID, int count, String userName);
    List<TaskBufferJsonModel> findOneDocumentForTaskByCririsID(Long crisisID, String userName, Integer maxresult);
}
