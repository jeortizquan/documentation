import models.DataDefinition
import com.liferay.portal.service.UserLocalServiceUtil
import nl.viking.controllers.Controller
import javax.portlet.PortletSession
import exceptions.EventException

record.temp = [:]
record.temp.urlPortal = h.themeDisplay.URLPortal
record.temp.groupUrl = h.themeDisplay.layout.getGroup().friendlyURL

// Get a data definition source
def getDDSource = { dd, pckg ->
    def groupId = h.themeDisplay.scopeGroupId;
    DataDefinition dataDef = DataDefinition.find("pckg,name,groupId", pckg, dd, groupId).get()
    dataDef.dataSource
}

if (form.name == "Start Process" || form.name == "Basic Information1" || form.name == "Basic Information") {
    def dataSource = getDDSource("Nawgegevens", "ContractProcess")
    def existentRecord = dataSource?.getRecord([filters: [email:record.email] ]).record
    dataDefinition.logger.info "Existent record: $existentRecord"
    if (existentRecord) {
        throw new EventException("Dit emailadres is reeds in de database aanwezig")
        return false
    }
}

def name = "${nawgegevens.roepnaam} ${nawgegevens.geboortenaam}/NAW gegevens".replaceAll("\\.", "_")
record.uploadDirectory = "/Arbeidscontracten/${name}"

return true


import com.mongodb.BasicDBObject
import nl.viking.db.MorphiaFactory
import org.apache.commons.lang3.StringUtils

// Check if the parameters
def tablename = "group10197.ContractProcess.Nawgegevens"
def filter = new BasicDBObject([
        userId : [
                '$nin' : [ null, ""]
        ],
        inDienst : [
                '$nin' : [ null, ""]
        ],
        roepnaam : [
                '$nin' : [ null, ""]
        ],
])

def projection = new BasicDBObject([
        "geslacht" : 1,
        "voorleters": 1,
        "roepnaam" : 1,
        "voorvogsel": 1,
        "geboortenaam": 1,
        "voorvoegselpartner" : 1,
        "achternaamPartner" : 1,
        "gebruikAchternaam" : 1,
        "geboorteplaats" : 1,
        "geboortedatum": 1,
        "bsnSofinummer": 1,
        "nationaliteit.nationaliteit": 1,
        "burgerlijkeStaat": 1,
        "adres": 1,
        "adres.straat" : 1,
        "adres.nr" : 1,
        "adres.postcode" : 1,
        "adres.plaats" : 1,
        "adres.nr" : 1,
        "land.land" : 1,
        "telefoon" : 1,
        "mobiel" : 1,
        "faciliteiten.mobiel" : 1,
        "email" : 1,
        "faciliteiten.emailadres" : 1,
        "faciliteiten.regiogroep.regiogroep" : 1,
        "userId" : 1,
        "typeContract" : 1,
        "eersteIndienst" : 1,
        "laatsteUitdienst" : 1,
        "typeContract": 1,
        "contracten": 1,
        "contracten.inDienst": 1,
        "contracten.uitDienst": 1,
        "contracten.soortDienstverband": 1,
        "contracten.werktijden": 1,
        "contracten.functieAfdeling": 1,
        "contracten.werktijden.afwijkendAantalUrenPerWeek": 1,
        "contracten.werktijden.einddatum": 1,
        "contracten.werktijden.ingangsdatum": 1,
        "contracten.functieAfdeling.einddatum": 1,
        "contracten.functieAfdeling.ingangsdatum": 1,
        "contracten.functieAfdeling.functie": 1,
        "_id" : 0,
        "inDienst" : 1
])
def nawgegevensMorphiaCollection = MorphiaFactory.ds().DB.getCollection(tablename)
def query = nawgegevensMorphiaCollection.find(filter,projection)
def queryResult = query.toList()
queryResult.each{ def nawGegevens ->
    nawGegevens.faciliteiten.emailadres =  StringUtils.lowerCase(nawGegevens.faciliteiten.emailadres)
    nawGegevens.roepnaam = StringUtils.stripAccents(nawGegevens.roepnaam).replace("'", "")
}
return [
        success: true,
        records: queryResult
]
