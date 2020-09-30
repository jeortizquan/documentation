import com.liferay.portlet.documentlibrary.service.DLAppServiceUtil
import com.liferay.portal.kernel.repository.model.FileEntry
import com.liferay.portal.service.ResourcePermissionLocalServiceUtil
import com.liferay.portal.model.*
import com.liferay.portlet.documentlibrary.model.DLFileEntry
import com.liferay.portal.service.RoleLocalServiceUtil
import com.liferay.portal.security.permission.ActionKeys
import com.liferay.portal.service.RepositoryLocalServiceUtil
import com.liferay.portal.service.GroupLocalServiceUtil
import com.liferay.portal.service.CompanyLocalServiceUtil
import com.liferay.portal.kernel.exception.PortalException
import java.text.SimpleDateFormat

def databag = [:]

def companies = CompanyLocalServiceUtil.getCompanies()

databag.companies = companies
companies.each {
    def company ->
        out.println("company Id: " + company.getCompanyId())
        out.println("company url: " + company.getPortalURL(company.getGroupId()))
        out.println("company name: " + company.getName())
        out.println("company group Id: " + company.getGroupId())
}

def load_repositories() {
    def repos = RepositoryLocalServiceUtil.getRepositoriesCount()

    out.println("repository count:" + repos)
    def repositories = RepositoryLocalServiceUtil.getRepositories(0, repos)
    databag.repositories = repositories
    repositories.each {
        def repository ->
            out.println("repository Id:" + repository.getRepositoryId())
            out.println("repository name:" + repository.getName())
            out.println("repository description:" + repository.getDescription())
    }
}

def groups_inventory() {
    def groupsCount = GroupLocalServiceUtil.getGroupsCount()
    out.println("group count:" + groupsCount)
    def groups = GroupLocalServiceUtil.getGroups(0, groupsCount)
    databag.groups = groups
    out.println("<table>")
    groups.each {
        def group ->
            out.println("<tr>")
            out.println("<td>group Id:" + group.getGroupId() + "</td>")
            out.println("<td>group IdName:" + group.getName() + "</td>")
            out.println("<td>group name:" + group.getDescriptiveName() + "</td>")
            out.println("<td>group description:" + group.getDescription() + "</td>")
            out.println("</tr>")
    }
    out.println("</table>")
}