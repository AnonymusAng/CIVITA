package co.edu.uniautonoma.civita.model
import org.junit.Assert.*
import org.junit.Test
class GeoHashTest {
    @Test fun standardVector() { assertEquals("u4pruydqq", GeoHash.encode(57.64911,10.40744)) }
    @Test fun zeroVector() { assertEquals("s00000000", GeoHash.encode(0.0,0.0)) }
    @Test(expected=IllegalArgumentException::class) fun rejectsNaN() { GeoHash.encode(Double.NaN,0.0) }
    @Test(expected=IllegalArgumentException::class) fun rejectsImpossibleLongitude() { GeoHash.encode(2.0,181.0) }
    @Test fun draftRequiresBothCoordinates() { assertNotNull(ReportDraft("obras","Un título","Descripción suficiente","Centro",2,2.44,null).validationError()) }
    @Test fun draftAcceptsReferenceWithoutInventingCoordinates() { val draft=ReportDraft("obras","Un título","Descripción suficiente","Centro",2); assertNull(draft.validationError()); assertNull(draft.latitude) }
}
