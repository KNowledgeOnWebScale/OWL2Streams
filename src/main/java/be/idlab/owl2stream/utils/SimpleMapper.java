package be.idlab.owl2stream.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class SimpleMapper {
	private String mapping;
	private boolean keepHeader = false;
	private boolean isFirst = true;
	private String header;
	private Map<String, MappingFunction> functions;
	private Map<String, String> functionsOnValue;

	public SimpleMapper(String mapping) {
		this.mapping = mapping;
		this.functions = new HashMap<String, MappingFunction>();
		this.functionsOnValue = new HashMap<String, String>();
	}

	public SimpleMapper(String mapping, boolean keepHeader) {
		this(mapping);
		this.keepHeader = keepHeader;
		
	}

	public static void main(String args[]) {
		String mapping = "?loc <hasvalue> ?avg.";
		String input = "loc,avg,\n"
				+ "https://igentprojectLBD#space_d6ea3a02-082e-4966-bcbf-563e69393f96,1^^http://www.w3.org/2001/XMLSchema#integer,\n"
				+ "https://igentprojectLBD#space_21b36f84-98e4-4689-924f-112fb8dd0925,1^^http://www.w3.org/2001/XMLSchema#integer,\n"
				+ "https://igentprojectLBD#space_21b36f84-98e4-4689-924f-112fb8dd0558,6^^http://www.w3.org/2001/XMLSchema#integer,\n"
				+ "https://igentprojectLBD#space_21b36f84-98e4-4689-924f-112fb8dd0cf0,1^^http://www.w3.org/2001/XMLSchema#integer,\n"
				+ "https://igentprojectLBD#space_a00c84ce-475e-4b66-98b3-72fbdf61761e,1^^http://www.w3.org/2001/XMLSchema#integer,";
		input = "loc,avg,\n" + "test,10,\n";
		mapping = "?loc <hasvalue> ?avg; <discrete> ?discrete.";
		SimpleMapper mapper = new SimpleMapper(mapping,true);
		mapper.registerFunction("discrete", "avg", new MappingFunction() {

			@Override
			public String apply(String input) {
				int parsedInt = Integer.parseInt(input);
				if (parsedInt < 5) {
					return "LowValue";
				} else if (parsedInt < 15) {
					return "MediumValue";
				} else if (parsedInt >= 15) {
					return "HighValue";
				}
				return null;
			}

		});
		String[] lines = input.split("\n");
		for(String line : lines) {
			String result = mapper.map(line);
			System.out.println(result);
		}
		
	}

	public void registerFunction(String name, String paramter, MappingFunction function) {
		this.functions.put(name, function);
		this.functionsOnValue.put(name, paramter);

	}

	public String map(String input) {
		if (keepHeader && isFirst) {
			isFirst = false;
			header = input;
			if (!functions.isEmpty()) {
				if(header.endsWith(",")) {
					header += String.join(",",functions.keySet());
				}else {
					header +=","+ String.join(",",functions.keySet());
				}
				
			}
			return null;
		} else {
			if (keepHeader) {
				StringBuilder builder = new StringBuilder();
				input = builder.append(header).append("\n").append(input).toString();
			}
			
			String[] lines = input.split("\n");
			String result = "";
			if (lines.length > 1) {
				String[] vars = lines[0].split(",");
				 
				
				for (int i = 1; i < lines.length; i++) {
					String currentMap = new String(mapping);
					String currentLine = lines[i];
					String[] variables = currentLine.split(",");
					if (!functions.isEmpty()) {
						for(Entry<String,MappingFunction> ent:functions.entrySet()) {
							String param = functionsOnValue.get(ent.getKey());
							int j =0;
							for(j = 0 ; j <vars.length;j++) {
								if(param.equals(vars[j])) {
									break;
								}
							}
							String newValue = ent.getValue().apply(variables[j]);
							if(currentLine.endsWith(",")) {
								currentLine +=newValue +",";
							}else {
								currentLine +=","+newValue;
							}
							
						}
					}
					variables = currentLine.split(",");
					for (int j = 0; j < vars.length; j++) {
						String var = vars[j];
						if (!var.equals("")) {
							currentMap = currentMap.replaceAll("\\?" + var, variables[j]);
						}
					}
					result += currentMap + "\n";
				}
			}
			return result;
		}
	}

}
