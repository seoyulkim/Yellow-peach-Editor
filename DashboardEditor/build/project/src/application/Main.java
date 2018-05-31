package application;
	
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.prefs.Preferences;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import application.model.Chart;
import application.util.Connect;
import application.view.BoardLayoutController;
import application.view.DraggableNode;
import application.view.ModifyDialogController;
import application.view.RootLayoutController;
import application.view.SelectVersionDialogController;
import application.view.SendDialogController;
import application.view.StreammingDialogController;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

public class Main extends Application {
	private Stage primaryStage;
	private BorderPane rootLayout;
	private AnchorPane boardLayout;
	private Scene scene;
	
	private RootLayoutController rootLayoutController;
	private BoardLayoutController boardLayoutController;
	
	// UI 리스트를 담는 대시보드 리스트	(tab아이디를 key로 사용하고 value로 차트리스트들을 가지고 있음)
	private HashMap<String, List<Chart>> dashBoardHashMap;
	
	// DashBoard 이름
	private String dashBoardName = "";
	
	// Topic List
	public static Vector<String> topicList;
	public static Vector<Vector<String>> dividedTopicList;
	
	public static Connect connect;
	public static boolean isDbConnect;
	
	public static String beforeTab = "0";
	
	public static int col = 0;
	public static int row = 0;
	
	public static HashMap<String, String> version;
	
	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		this.primaryStage.setTitle("Editor");
		this.primaryStage.getIcons().add(new Image("file:resources/images/mars.png"));
		
		connect = new Connect();
		if(isDbConnect = checkDBConnect()) {
			topicList = connect.getTopicList();
			topicListSeparator();
		}
		// 대시보드 hashMap 생성.
		dashBoardHashMap = new HashMap<String, List<Chart>>();
		version = new HashMap<String, String>();
		
		Group root = new Group();
		
		Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
		primaryStage.setWidth(primaryScreenBounds.getWidth());
		primaryStage.setHeight(primaryScreenBounds.getHeight());
		primaryStage.setX(primaryScreenBounds.getMinX());
		primaryStage.setY(primaryScreenBounds.getMinY());
		
		initRootLayout();
		showBoardLayout();
		
		primaryStage.show();
		
		boardLayoutController.setSize(primaryStage.getWidth(), primaryStage.getHeight());
		
		double dashBoardPaneWidth = primaryScreenBounds.getWidth()-boardLayoutController.getMenuRootPane().getPrefWidth();
		double dashBoardPaneHeight = primaryScreenBounds.getHeight()-117;
		
		rootLayoutController.setDashBoardPaneWidth(dashBoardPaneWidth);
		rootLayoutController.setDashBoardPaneHeight(dashBoardPaneHeight);
		
		rootLayoutController.setBoardController(boardLayoutController);
		rootLayoutController.handleNew();
		
		scene.getAccelerators().put(new KeyCodeCombination(
				KeyCode.S, KeyCombination.CONTROL_ANY), new Runnable() {
					@Override public void run() {
						rootLayoutController.handleSave();
					}
				});
	}
	
	public void initRootLayout() {
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(Main.class.getResource("fxml/RootLayout.fxml"));
			rootLayout = (BorderPane) loader.load();
			scene = new Scene(rootLayout);
			primaryStage.setScene(scene);
			
			// 컨트롤러한테 Main 접근 권한을 준다.
			rootLayoutController = loader.getController();
			rootLayoutController.setMain(this);
			
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void showBoardLayout() {
		try {
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(Main.class.getResource("fxml/BoardLayout.fxml"));
			boardLayout = (AnchorPane) loader.load();
			
			rootLayout.setCenter(boardLayout);
			
			// 컨트롤러한테 Main 접근 권한을 준다.
			boardLayoutController = loader.getController();
			boardLayoutController.setMain(this);
						
			// BoardLayout의 크기 지정
			boardLayoutController.getBorderPane().setPrefWidth(primaryStage.getWidth()-18);
			boardLayoutController.getBorderPane().setPrefHeight(primaryStage.getHeight()-79);
			
			// menuPane의 크기 지정
			boardLayoutController.getMenuRootPane().setMinWidth(primaryStage.getWidth()*0.08);
		
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * UI 파일 환경설정을 반환한다.
	 * 즉 파일은 마지막으로 열린 것이고, 환경설정은 OS 특정 레지스트리로부터 읽는다.
	 * 만일 preference를 찾지 못하면 null을 반환한다.
	 *
	 * @return
	 */
	public File getUiFilePath() {
	    Preferences prefs = Preferences.userNodeForPackage(Main.class);
	    String filePath = prefs.get("filePath", null);
	    String currentTab = boardLayoutController.getTabPane().getSelectionModel().getSelectedItem().getId();
	    //System.out.println(filePath);
	    System.out.println("c : "+currentTab);
	    System.out.println("b : "+beforeTab);
	    if (filePath != null) {
	    	if(!currentTab.equals(beforeTab))
	    		return null;
	        return new File(filePath);
	    } else {
	        return null;
	    }
	}
	/**
	 * 현재 불러온 파일의 경로를 설정한다. 이 경로는 OS 특정 레지스트리에 저장된다.
	 *
	 * @param file the file or null to remove the path
	 */
	public void setUiFilePath(File file) {
	    Preferences prefs = Preferences.userNodeForPackage(Main.class);
	    beforeTab = boardLayoutController.getTabPane().getSelectionModel().getSelectedItem().getId();
	    if (file != null) {
	        prefs.put("filePath", file.getPath());

	        // Tab 타이틀을 업데이트한다.
	        Tab tab = boardLayoutController.getTabPane().getSelectionModel().getSelectedItem();
	        int idx = file.getName().indexOf(".");
	        tab.setText(file.getName().substring(0, idx));
	    } else {
	        prefs.remove("filePath");
	    }    
	}
	
	/**
	 * 현재 대시보드를 지정한 파일에 저장한다.
	 *
	 * @param file
	 */
	public void saveUiDataToFile(File file) {
	    try {
	    	System.out.println("save DashBoard");

			toJson(dashBoardHashMap, boardLayoutController.getTabId(), file);

	        // 파일 경로를 레지스트리에 저장한다.
	        setUiFilePath(file);
	        	        
	    } catch (Exception e) { // 예외를 잡는다.
	        Alert alert = new Alert(AlertType.ERROR);
	        alert.setTitle("Error");
	        alert.setHeaderText("Could not save data");
	        alert.setContentText("Could not save data to file:\n" + file.getPath());

	        alert.showAndWait();
	    }
	}

    public void setChartNumber() {
    	Tab tab = boardLayoutController.getTabPane().getSelectionModel().getSelectedItem();
		AnchorPane a = (AnchorPane)tab.getContent();
        ScrollPane s = (ScrollPane)a.getChildren().get(0);
        FlowPane fp = (FlowPane)s.getContent();
        
        ObservableList<Node> workingCollection;
        if(version.get(tab.getId()).equals("PC")) {
        	// tabFlowPane에 있는 자식 노드 리스트
    		workingCollection = FXCollections.observableArrayList(
    				fp.getChildren()
             );        	
        }else {
        	FlowPane backFp = (FlowPane) fp.getChildren().get(0);
        	// tabFlowPane에 있는 자식 노드 리스트
    		workingCollection = FXCollections.observableArrayList(
    				backFp.getChildren()
             );
        }
        int rowCount = 0;
        int colCount = 0;
        for (int i = 0; i < workingCollection.size(); i++) {
        	DraggableNode node = (DraggableNode)workingCollection.get(i);
        	node.getChart().setNumber(i);
        	
        	if(i == 0) {	// 첫 노드
        		node.getChart().setRow(0);
        		node.getChart().setCol(0);
        		if(node.getChart().getType().equals("value") ||
        			node.getChart().getType().equals("image") ||
        			node.getChart().getType().equals("text"))
        			colCount++;
        		else 
        			colCount += 2;
        	}else { // 두번째 노드부터~
        		// 큰 타입이 넘어갈 때
        		if(!(node.getChart().getType().equals("value") ||
        				node.getChart().getType().equals("image") ||
            			node.getChart().getType().equals("text"))
        				&& colCount >= 5) {
        			rowCount++;
        			colCount = 0;
        		}else if((node.getChart().getType().equals("value") ||
        				node.getChart().getType().equals("image") ||
            			node.getChart().getType().equals("text"))
        				&& colCount >= 6) {	// 작은 타입이 넘어갈 때
        			rowCount++;
        			colCount = 0;
        		}
        		// 먼저 설정
        		node.getChart().setRow(rowCount);
        		node.getChart().setCol(colCount);
        		if(node.getChart().getType().equals("value") ||
            			node.getChart().getType().equals("image") ||
            			node.getChart().getType().equals("text")) {
            			colCount++;
        		}else { 
            			colCount += 2;
        		}
        		
        	}
		}
        
        
    }
	
	// json 데이터로 변환
	private void toJson(HashMap<String, List<Chart>> dashBoardHashMap, String tabId, File file) {
		System.out.println("converting start");
		setChartNumber();
		
		JSONObject obj = new JSONObject();
		
		List<Chart> charts = dashBoardHashMap.get(tabId);
		System.out.println("charts size = "+charts.size());
		
		// insert "dashboardName" : "bb",
		int idx = file.getName().indexOf(".");
		dashBoardName = file.getName().substring(0, idx);
		obj.put("dashboardName", dashBoardName);
		
		// insert "content":[]
		JSONArray jsonArray = new JSONArray();
		Map map = new LinkedHashMap(charts.size());
		for (int i = 0; i < charts.size(); i++) {
			Map m = new LinkedHashMap(11);	// chart
			
			m.put("chart", charts.get(i).getType());
			m.put("chartName", charts.get(i).getChartName());
			m.put("firstTopic", charts.get(i).getFirstTopic());
			m.put("firstName", charts.get(i).getFirstName());
			m.put("secondTopic", charts.get(i).getSecondTopic());
			m.put("secondName", charts.get(i).getSecondName());
			m.put("firstControl", charts.get(i).getFirstControl());
			m.put("secondControl", charts.get(i).getSecondControl());
			m.put("row", charts.get(i).getRow());
			m.put("level", charts.get(i).getLevel());
			m.put("unit", charts.get(i).getUnit());
			//m.put("publishTopic", charts.get(i).getPublishTopic());
			
			//System.out.println(m);
			
			map.put(charts.get(i).getNumber(), m);
			
		}
		jsonArray.add(map);
		obj.put("content", jsonArray);
		System.out.println(obj);
		
		//String fileName = "json/".concat(dashBoardName).concat(".json");
		try(FileWriter fileWrite = new FileWriter(file)){
			fileWrite.write(obj.toJSONString());
			fileWrite.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Information");
		alert.setHeaderText(null);
		alert.setContentText("json파일로 저장되었습니다.");
	}
	
	// DB연결 확인
	public boolean checkDBConnect() {
		JSONParser parser = new JSONParser();
		String url, user, pass, camera;
		try {
			Object obj = parser.parse(new FileReader("dbData.json"));
			JSONObject jsonObject = (JSONObject) obj;
			
			url = (String)jsonObject.get("url");
			user = (String)jsonObject.get("user");
			pass = (String)jsonObject.get("pass");
			camera = (String)jsonObject.get("camera");

			connect.getPool().set_url(url);
			connect.getPool().set_user(user);
			connect.getPool().set_password(pass);
			
			try {
				if(connect.getPool().createConnection() != null) {
					//System.out.println("connect");
					isDbConnect = true;
					topicList = connect.getTopicList();
					topicListSeparator();
					return true;
				}
				
			} catch (SQLException e) {
				System.out.println("connecting false");
				isDbConnect = false;
				return false;
			}
			
		} catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
		return false;
	}
	
	// dividedTopicList에 토픽 리스트를 각각 분할하여 저장
	public void topicListSeparator(){
		// 분할 벡터 생성
		dividedTopicList = new Vector<Vector<String>>();
		dividedTopicList.add(new Vector<String>());
		dividedTopicList.add(new Vector<String>());
		dividedTopicList.add(new Vector<String>());
		for (int i = 0; i < topicList.size(); i++) {
			int c = 0;
			String topic = topicList.get(i);
			StringTokenizer st = new StringTokenizer(topic, "/");
			dividedTopicList.get(c++).add(st.nextToken());
			dividedTopicList.get(c++).add(st.nextToken());
			dividedTopicList.get(c++).add(st.nextToken());
		}
	}
	
	// 토픽 리스트 session 개수 반환
	public int getTopicListSessionCount() {
		int max = 0;
		for (int i = 0; i < topicList.size(); i++) {
			String topic = topicList.get(i);
			StringTokenizer st = new StringTokenizer(topic, "/");
			int n = st.countTokens();
			if(max < n)
				max = n;
		}
		return max;
	}
	
	
	// 차트 리스트 반환
	public HashMap<String, List<Chart>> getDashBoardHashMap(){
		return dashBoardHashMap;
	}
	
	public void setRootLayout(BorderPane rootLayout) {
		this.rootLayout = rootLayout;
	}
	public void setRootLayoutController(RootLayoutController rootLayoutController) {
		this.rootLayoutController = rootLayoutController;
	}
	public RootLayoutController getRootLayoutController() {
		return rootLayoutController;
	}
    public BoardLayoutController getBoardLayoutController() {
    	return boardLayoutController;
    }
	
	public Scene getScene() {
		return scene;
	}
	
	public Stage getPrimaryStage() {
		return primaryStage;
	}
	
	public static HashMap<String, String> getVersion() {
		return version;
	}


	public static void main(String[] args) {
		launch(args);
	}
}
