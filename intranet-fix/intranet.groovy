import com.liferay.portlet.documentlibrary.service.DLAppServiceUtil
import com.liferay.portal.kernel.repository.model.FileEntry
import com.liferay.portal.service.ResourcePermissionLocalServiceUtil
import com.liferay.portal.model.*
import com.liferay.portlet.documentlibrary.model.DLFileEntry
import com.liferay.portal.service.RoleLocalServiceUtil
import com.liferay.portal.security.permission.ActionKeys
import com.liferay.portal.service.GroupLocalServiceUtil
import com.liferay.portal.kernel.exception.PortalException
import java.text.SimpleDateFormat

def databag = [:]

/* Important variable */
databag.companyId = 10157; // Production
//databag.groupnames = ["Rotterdam", "Noorden","Veluwe" ,"Zwolle - Kampen","Oosten","Zuid - West","Midden - Noord"]
databag.groupnames = ["Personeelszaken"]
//databag.groupnames = ["Midden - Noord"]
databag.foldernames = ["Arbeidscontracten","tmp","undefined"]
databag.dryrun = false

// Setup the file log
final def SCRIPT_ID = "MOVING_DATA"
def startdate = new Date();
def startdateText = new SimpleDateFormat("yyyyMMdd-HHmm").format(startdate)
outputFile = new File("""${System.getProperty("liferay.home")}/scripting/out-${SCRIPT_ID}-${startdateText}.log""")
outputFile.getParentFile().mkdirs()

outputFile << "START_DATE_TIME: " + new SimpleDateFormat().format(startdate) + "\n\n"

/* Get role */
databag.siteMemberRole = RoleLocalServiceUtil.getRole(databag.companyId, RoleConstants.SITE_MEMBER)
databag.guestRole = RoleLocalServiceUtil.getRole(databag.companyId, RoleConstants.GUEST)
databag.userRole = RoleLocalServiceUtil.getRole(databag.companyId, RoleConstants.USER)

long FOLDER_ID = 0;
long folderId = 2653312;
out.println  "demo dit is a bericht"

// check all the groups
databag.groupnames.each{ def groupname ->
	// Get group
	def group = GroupLocalServiceUtil.getGroup(databag.companyId,groupname)
	outputFile << ("=====================================\n\n")
	outputFile << ("GROUP_ID: " + group.groupId + "\n\n")
	outputFile << ("RUN_DRY: " + databag.dryrun + "\n\n")

	// We go throughout all the folder

	databag.foldernames.each { def foldername ->
		try{
			def folder = DLAppServiceUtil.getFolder(group.groupId,0,foldername)
			out.println("companyId:" + folder.getCompanyId())
			//processFolder(folder,databag)
		}
		catch(PortalException ex){
			outputFile << "************\n"
			outputFile << ("ERROR: process folder ${foldername}: ${ex.message} \n")
		}
		
	}
}

def enddate = new Date();
outputFile << "\nEND_DATE_TIME: " + new SimpleDateFormat().format(enddate) + "\n"

