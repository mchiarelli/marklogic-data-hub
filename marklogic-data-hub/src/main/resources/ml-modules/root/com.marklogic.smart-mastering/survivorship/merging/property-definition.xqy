xquery version "1.0-ml";

(:~
 : Implementation library for building property definitions for merging.
 :
 :)
module namespace prop-def = "http://marklogic.com/smart-mastering/survivorship/property-definition";


declare function prop-def:new() as map:map
{
  map:map()
};

declare function prop-def:with-values($prop-def as map:map, $values as item()*) as map:map
{
  $prop-def => map:with("values", $values)
};

declare function prop-def:with-sources($prop-def as map:map, $sources as item()*) as map:map
{
  $prop-def => map:with("sources", $sources)
};

declare function prop-def:with-name($prop-def as map:map, $name as xs:QName) as map:map
{
  $prop-def => map:with("name", $name)
};

declare function prop-def:with-path($prop-def as map:map, $path as xs:string?) as map:map
{
  $prop-def => map:with("path", $path)
};

declare function prop-def:with-algorithm-info($prop-def as map:map, $algorithm-info as item()*) as map:map
{
  $prop-def => map:with("algorithm", $algorithm-info)
};

declare function prop-def:with-retain-array($prop-def as map:map, $retain-array as xs:boolean) as map:map
{
  $prop-def => map:with("retainArray", $retain-array)
};

declare function prop-def:with-namespaces($prop-def as map:map, $namespaces as map:map?) as map:map
{
  $prop-def => map:with("namespaces", $namespaces)
};

declare function prop-def:with-extensions($prop-def as map:map, $extensions as map:map?) as map:map
{
  map:new((
    $prop-def,
    $extensions
  ))
};
