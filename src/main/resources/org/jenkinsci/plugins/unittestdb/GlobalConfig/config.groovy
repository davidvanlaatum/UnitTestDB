package com.vanlaatum.unittestsdb.GlobalConfig;

def f = namespace(lib.FormTagLib)

f.section(title:"Database Configuration for Unit Test DB") {
  f.dropdownDescriptorSelector(field:"database",title:_("Database"))
}
